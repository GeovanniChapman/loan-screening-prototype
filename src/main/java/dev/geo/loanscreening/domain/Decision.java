package dev.geo.loanscreening.domain;

/**
 * Outcome of the rules engine for one loan application.
 *
 * <p><b>Java 21 concepts shown here</b>
 * <ul>
 *   <li><b>Sealed interface</b>: the compiler knows the <em>complete</em> list of permitted
 *       implementations, so a {@code switch} over a {@code Decision} needs no {@code default}
 *       branch. Adding a new outcome later turns every incomplete {@code switch} into a
 *       compile error instead of a runtime surprise.</li>
 *   <li><b>Records</b>: each outcome is an immutable value carrier with generated
 *       {@code equals/hashCode/toString}.</li>
 *   <li><b>Pattern matching for {@code switch} + record patterns</b>: see {@link #summary()},
 *       which deconstructs the record components directly in the {@code case} label.</li>
 * </ul>
 */
public sealed interface Decision {

    /** Human-readable justification, stored with the application for auditing. */
    String reason();

    /** The application meets every criterion and can proceed automatically. */
    record Approved(String reason) implements Decision {}

    /** A borderline profile: a human analyst must take the final call. */
    record ManualReview(String reason) implements Decision {}

    /** The application violates a hard rule and is declined. */
    record Rejected(String reason) implements Decision {}

    /**
     * Maps the decision to the persisted lifecycle status.
     *
     * <p>Exhaustive switch <em>expression</em> over a sealed hierarchy: no {@code default}
     * needed, and the compiler verifies that every permitted subtype is covered.
     */
    default ApplicationStatus status() {
        return switch (this) {
            case Approved a -> ApplicationStatus.APPROVED;
            case ManualReview m -> ApplicationStatus.MANUAL_REVIEW;
            case Rejected r -> ApplicationStatus.REJECTED;
        };
    }

    /**
     * One-line description for logs. Uses <b>record patterns</b> to extract the
     * {@code reason} component without calling the accessor explicitly.
     */
    default String summary() {
        return switch (this) {
            case Approved(var reason) -> "APPROVED (%s)".formatted(reason);
            case ManualReview(var reason) -> "MANUAL REVIEW (%s)".formatted(reason);
            case Rejected(var reason) -> "REJECTED (%s)".formatted(reason);
        };
    }
}
