package dev.geo.loanscreening.web;

import dev.geo.loanscreening.service.SubmitLoanCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * JSON body of {@code POST /api/applications}. Bean Validation annotations on the record
 * components are enforced by {@code @Valid} in the controller.
 */
public record SubmitLoanRequest(
        @NotBlank @Size(max = 120) String applicantName,
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal annualIncome,
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal requestedAmount,
        @Min(300) @Max(850) int creditScore) {

    SubmitLoanCommand toCommand() {
        return new SubmitLoanCommand(applicantName, annualIncome, requestedAmount, creditScore);
    }
}
