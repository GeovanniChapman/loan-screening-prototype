package dev.geo.loanscreening.service;

import java.util.UUID;

/**
 * Domain event published inside the "submit" transaction. A listener reacts <em>after the commit</em>
 * (see {@code LoanEvaluationScheduler}), which guarantees the job can never start before the row
 * it needs is visible.
 */
public record LoanApplicationSubmitted(UUID applicationId) {
}
