package dev.geo.loanscreening.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class LoanApplicationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static LoanApplication newApplication() {
        return LoanApplication.submit("Ada", new BigDecimal("50000"), new BigDecimal("10000"), 700, NOW);
    }

    @Test
    void startsPending() {
        var application = newApplication();

        assertThat(application.getId()).isNotNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(application.getCreatedAt()).isEqualTo(NOW);
        assertThat(application.getEvaluatedAt()).isNull();
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> LoanApplication.submit("Ada", BigDecimal.ZERO, BigDecimal.TEN, 700, NOW));
        assertThatIllegalArgumentException().isThrownBy(
                () -> LoanApplication.submit("Ada", BigDecimal.TEN, new BigDecimal("-1"), 700, NOW));
    }

    @Test
    void appliesDecisionOnce() {
        var application = newApplication();
        var later = NOW.plusSeconds(5);

        application.applyDecision(new Decision.Approved("fine"), later);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(application.getDecisionReason()).isEqualTo("fine");
        assertThat(application.getEvaluatedAt()).isEqualTo(later);
        assertThatIllegalStateException().isThrownBy(
                () -> application.applyDecision(new Decision.Rejected("too late"), later));
    }
}
