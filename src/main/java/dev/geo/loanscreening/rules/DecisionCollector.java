package dev.geo.loanscreening.rules;

import dev.geo.loanscreening.domain.Decision;

import java.util.ArrayList;
import java.util.Optional;
import java.util.SequencedCollection;

/**
 * Drools <em>global</em> where rules report their verdicts.
 *
 * <p>Every rule whose conditions match fires, in descending {@code salience} order, and calls one
 * of {@link #approve}, {@link #manualReview} or {@link #reject}. Because the verdicts are kept
 * in a Java 21 {@link SequencedCollection}, "the verdict of the highest-priority rule" is simply
 * {@link SequencedCollection#getFirst()}: the collection's encounter order <em>is</em> the
 * priority order. (Before Java 21 this was {@code list.get(0)}, which is easy to get wrong on
 * an empty list and does not express intent.)
 *
 * <p>Not thread-safe by design: a new instance is created for every evaluation.
 */
public final class DecisionCollector {

    private final SequencedCollection<Decision> verdicts = new ArrayList<>();

    /** Called from the DRL. */
    public void approve(String reason) {
        verdicts.add(new Decision.Approved(reason));
    }

    /** Called from the DRL. */
    public void manualReview(String reason) {
        verdicts.add(new Decision.ManualReview(reason));
    }

    /** Called from the DRL. */
    public void reject(String reason) {
        verdicts.add(new Decision.Rejected(reason));
    }

    /** @return the verdict of the first (highest-salience) rule that fired, if any fired */
    public Optional<Decision> winner() {
        return verdicts.isEmpty() ? Optional.empty() : Optional.of(verdicts.getFirst());
    }

    /** @return every verdict in firing order, newest last. Useful for diagnostics. */
    public SequencedCollection<Decision> verdicts() {
        return java.util.Collections.unmodifiableSequencedCollection(verdicts);
    }
}
