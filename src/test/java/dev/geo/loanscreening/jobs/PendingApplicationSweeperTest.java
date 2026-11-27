package dev.geo.loanscreening.jobs;

import dev.geo.loanscreening.config.ScreeningProperties;
import dev.geo.loanscreening.domain.ApplicationStatus;
import dev.geo.loanscreening.domain.LoanApplication;
import dev.geo.loanscreening.domain.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingApplicationSweeperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Mock LoanApplicationRepository repository;
    @Mock LoanEvaluationScheduler scheduler;

    PendingApplicationSweeper sweeper;

    @BeforeEach
    void setUp() {
        sweeper = new PendingApplicationSweeper(repository, scheduler,
                new ScreeningProperties(Duration.ofMinutes(2)), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void reschedulesStaleApplications() {
        var stale = LoanApplication.submit("Ada", BigDecimal.TEN, BigDecimal.ONE, 700, NOW.minusSeconds(600));
        when(repository.findByStatusAndCreatedAtBefore(ApplicationStatus.PENDING, NOW.minusSeconds(120)))
                .thenReturn(List.of(stale));

        sweeper.sweep();

        verify(scheduler).schedule(stale.getId());
    }

    @Test
    void doesNothingWhenNoneAreStale() {
        when(repository.findByStatusAndCreatedAtBefore(ApplicationStatus.PENDING, NOW.minusSeconds(120)))
                .thenReturn(List.of());

        sweeper.sweep();

        verifyNoInteractions(scheduler);
    }
}
