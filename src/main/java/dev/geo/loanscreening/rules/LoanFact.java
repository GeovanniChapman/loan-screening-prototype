package dev.geo.loanscreening.rules;

import dev.geo.loanscreening.domain.LoanApplication;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The <em>fact</em> inserted into Drools: only the data the rules need, in simple types.
 *
 * <p>Keeping the fact separate from the JPA entity means rules cannot accidentally mutate
 * persistent state, and the DRL stays independent from the persistence model. Being a
 * Java 21 {@code record}, it is immutable; Drools reads it through the record accessors
 * ({@code creditScore()}, {@code loanToIncomeRatio()}).
 *
 * @param creditScore       bureau score, typically 300-850
 * @param loanToIncomeRatio requested amount divided by annual income (e.g. {@code 2.5})
 */
public record LoanFact(int creditScore, double loanToIncomeRatio) {

    /** Builds the fact from an entity, pre-computing the ratio so the DRL stays trivial. */
    public static LoanFact from(LoanApplication application) {
        double ratio = application.getRequestedAmount()
                .divide(application.getAnnualIncome(), 4, RoundingMode.HALF_UP)
                .doubleValue();
        return new LoanFact(application.getCreditScore(), ratio);
    }

    /** Convenience for tests and callers that do not have an entity. */
    public static LoanFact of(int creditScore, BigDecimal income, BigDecimal amount) {
        return new LoanFact(creditScore,
                amount.divide(income, 4, RoundingMode.HALF_UP).doubleValue());
    }
}
