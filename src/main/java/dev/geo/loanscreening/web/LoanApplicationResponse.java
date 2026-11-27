package dev.geo.loanscreening.web;

import dev.geo.loanscreening.domain.ApplicationStatus;
import dev.geo.loanscreening.domain.LoanApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** JSON representation of a {@link LoanApplication}. Decouples the API from the JPA entity. */
public record LoanApplicationResponse(
        UUID id,
        String applicantName,
        BigDecimal annualIncome,
        BigDecimal requestedAmount,
        int creditScore,
        ApplicationStatus status,
        String decisionReason,
        Instant createdAt,
        Instant evaluatedAt) {

    static LoanApplicationResponse from(LoanApplication a) {
        return new LoanApplicationResponse(a.getId(), a.getApplicantName(), a.getAnnualIncome(),
                a.getRequestedAmount(), a.getCreditScore(), a.getStatus(), a.getDecisionReason(),
                a.getCreatedAt(), a.getEvaluatedAt());
    }
}
