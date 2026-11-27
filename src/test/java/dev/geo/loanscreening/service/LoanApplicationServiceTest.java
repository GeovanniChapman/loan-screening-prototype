package dev.geo.loanscreening.service;

import dev.geo.loanscreening.domain.ApplicationStatus;
import dev.geo.loanscreening.domain.Decision;
import dev.geo.loanscreening.domain.LoanApplication;
import dev.geo.loanscreening.domain.LoanApplicationRepository;
import dev.geo.loanscreening.rules.LoanFact;
import dev.geo.loanscreening.rules.LoanRiskRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pure unit test: collaborators are mocked, the clock is fixed. */
@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Mock LoanApplicationRepository repository;
    @Mock LoanRiskRuleEngine ruleEngine;
    @Mock ApplicationEventPublisher events;

    LoanApplicationService service;

    @BeforeEach
    void setUp() {
        service = new LoanApplicationService(repository, ruleEngine, events, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static LoanApplication pendingApplication() {
        return LoanApplication.submit("Ada", new BigDecimal("50000"), new BigDecimal("10000"), 700, NOW);
    }

    @Test
    void submitSavesAsPendingAndPublishesEvent() {
        var command = new SubmitLoanCommand("Ada", new BigDecimal("50000"), new BigDecimal("10000"), 700);

        LoanApplication saved = service.submit(command);

        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        verify(repository).save(saved);
        verify(events).publishEvent(new LoanApplicationSubmitted(saved.getId()));
    }

    @Test
    void evaluateAppliesTheRulesVerdict() {
        var application = pendingApplication();
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));
        when(ruleEngine.evaluate(any(LoanFact.class))).thenReturn(new Decision.Approved("ok"));

        service.evaluate(application.getId());

        // 10,000 / 50,000 = 0.2 loan-to-income ratio, credit score 700
        var factCaptor = ArgumentCaptor.forClass(LoanFact.class);
        verify(ruleEngine).evaluate(factCaptor.capture());
        assertThat(factCaptor.getValue()).isEqualTo(new LoanFact(700, 0.2));
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(application.getEvaluatedAt()).isEqualTo(NOW);
    }

    @Test
    void evaluateIsIdempotentForDecidedApplications() {
        var application = pendingApplication();
        application.applyDecision(new Decision.Rejected("earlier"), NOW);
        when(repository.findById(application.getId())).thenReturn(Optional.of(application));

        service.evaluate(application.getId());

        verifyNoInteractions(ruleEngine);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void evaluateFailsForUnknownId() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluate(id)).isInstanceOf(ApplicationNotFoundException.class);
        verify(ruleEngine, never()).evaluate(any());
    }

    @Test
    void recentClampsTheLimit() {
        service.recent(10_000);
        service.recent(-5);

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, org.mockito.Mockito.times(2)).findAllByOrderByCreatedAtDesc(pageable.capture());
        assertThat(pageable.getAllValues()).extracting(Pageable::getPageSize)
                .containsExactly(LoanApplicationService.MAX_PAGE_SIZE, 1);
    }
}
