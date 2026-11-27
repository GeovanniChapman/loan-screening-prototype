package dev.geo.loanscreening.jobs;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEvaluationSchedulerTest {

    @Test
    void jobIdIsDeterministicPerApplication() {
        var applicationId = UUID.randomUUID();

        assertThat(LoanEvaluationScheduler.jobIdFor(applicationId))
                .isEqualTo(LoanEvaluationScheduler.jobIdFor(applicationId))
                .isNotEqualTo(LoanEvaluationScheduler.jobIdFor(UUID.randomUUID()));
    }
}
