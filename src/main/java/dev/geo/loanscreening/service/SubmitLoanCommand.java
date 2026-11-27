package dev.geo.loanscreening.service;

import java.math.BigDecimal;

/** Input of the "submit application" use case, independent from the HTTP representation. */
public record SubmitLoanCommand(String applicantName, BigDecimal annualIncome,
                                BigDecimal requestedAmount, int creditScore) {
}
