package dev.geo.loanscreening.domain;

/** Lifecycle of a {@link LoanApplication}. */
public enum ApplicationStatus {

    /** Saved, waiting for the background job to run the rules. */
    PENDING,

    /** Automatically approved by the rules. */
    APPROVED,

    /** Needs a human analyst. */
    MANUAL_REVIEW,

    /** Automatically declined by the rules. */
    REJECTED;

    /** @return {@code true} once the rules have produced a verdict. */
    public boolean isFinal() {
        return this != PENDING;
    }
}
