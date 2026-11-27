package dev.geo.loanscreening.rules;

import dev.geo.loanscreening.domain.Decision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionCollectorTest {

    @Test
    void hasNoWinnerWhenNothingFired() {
        assertThat(new DecisionCollector().winner()).isEmpty();
    }

    @Test
    void firstVerdictWinsAndOrderIsKept() {
        var collector = new DecisionCollector();
        collector.reject("first");
        collector.manualReview("second");
        collector.approve("third");

        assertThat(collector.winner()).contains(new Decision.Rejected("first"));
        // SequencedCollection (Java 21): getLast() / reversed() are available on the view too.
        assertThat(collector.verdicts().getLast()).isEqualTo(new Decision.Approved("third"));
        assertThat(collector.verdicts().reversed().getFirst()).isEqualTo(new Decision.Approved("third"));
    }

    @Test
    void exposedVerdictsAreReadOnly() {
        var collector = new DecisionCollector();
        collector.approve("ok");

        assertThatThrownBy(() -> collector.verdicts().addLast(new Decision.Rejected("hack")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
