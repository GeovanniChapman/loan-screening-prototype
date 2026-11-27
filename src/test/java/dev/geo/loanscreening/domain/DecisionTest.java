package dev.geo.loanscreening.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionTest {

    @Test
    void mapsEachOutcomeToItsStatus() {
        assertThat(new Decision.Approved("r").status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(new Decision.ManualReview("r").status()).isEqualTo(ApplicationStatus.MANUAL_REVIEW);
        assertThat(new Decision.Rejected("r").status()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void summaryUsesRecordPatterns() {
        assertThat(new Decision.Approved("good").summary()).isEqualTo("APPROVED (good)");
        assertThat(new Decision.ManualReview("hmm").summary()).isEqualTo("MANUAL REVIEW (hmm)");
        assertThat(new Decision.Rejected("bad").summary()).isEqualTo("REJECTED (bad)");
    }

    @Test
    void recordsHaveValueEquality() {
        assertThat(new Decision.Approved("x")).isEqualTo(new Decision.Approved("x"));
        assertThat(new Decision.Approved("x")).isNotEqualTo(new Decision.Rejected("x"));
    }
}
