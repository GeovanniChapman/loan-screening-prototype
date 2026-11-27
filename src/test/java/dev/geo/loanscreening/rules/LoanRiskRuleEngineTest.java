package dev.geo.loanscreening.rules;

import dev.geo.loanscreening.domain.Decision;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the DRL through the real Drools engine (no Spring context, no mocks): they run in
 * milliseconds and prove the <em>business rules</em>, which is what matters most here.
 */
class LoanRiskRuleEngineTest {

    // Compiling the DRL is the expensive part, so one engine is shared by all tests.
    // This is safe because the engine is thread-safe (see the concurrency test below).
    private static LoanRiskRuleEngine engine;

    @BeforeAll
    static void compileRulesOnce() {
        engine = new LoanRiskRuleEngine();
    }

    /**
     * Table-driven test. A Java 15+ text block keeps the table readable.
     * Columns: creditScore, loanToIncomeRatio, expected outcome type, note (documentation only).
     */
    @ParameterizedTest(name = "score={0}, ratio={1} -> {2}")
    @CsvSource(textBlock = """
            # score, ratio, expected, note
            800, 1.0, Approved, comfortable profile
            650, 5.0, Approved, boundary: both limits are inclusive for approval
            649, 1.0, ManualReview, boundary: just below 650
            800, 5.01, ManualReview, boundary: just above 5x income
            500, 1.0, ManualReview, boundary: 500 is NOT rejected
            499, 1.0, Rejected, boundary: just below 500
            800, 10.0, ManualReview, boundary: exactly 10x is NOT rejected
            800, 10.01, Rejected, boundary: just above 10x income
            300, 20.0, Rejected, both hard rules match
            """)
    @DisplayName("applies the expected rule at every boundary")
    void evaluatesBoundaries(int creditScore, double ratio, String expected, String note) {
        Decision decision = engine.evaluate(new LoanFact(creditScore, ratio));

        assertThat(decision.getClass().getSimpleName()).isEqualTo(expected);
        assertThat(decision.reason()).isNotBlank();
    }

    @Test
    @DisplayName("builds the ratio from money amounts")
    void computesRatioFromAmounts() {
        var fact = LoanFact.of(700, new BigDecimal("40000"), new BigDecimal("100000"));

        assertThat(fact.loanToIncomeRatio()).isEqualTo(2.5);
        assertThat(engine.evaluate(fact)).isInstanceOf(Decision.Approved.class);
    }

    @Test
    @DisplayName("the highest-salience rule wins when several rules match")
    void highestSalienceWins() {
        // Matches "Reject - credit score" (100), "Manual review" (50): rejection must win.
        Decision decision = engine.evaluate(new LoanFact(400, 1.0));

        assertThat(decision).isEqualTo(new Decision.Rejected("Credit score below 500"));
    }

    /**
     * One engine, 1,000 evaluations on 1,000 <b>virtual threads</b> (Java 21). Proves the shared
     * KieBase can be used concurrently and that every call gets its own isolated result.
     */
    @Test
    @DisplayName("is thread-safe under heavy concurrent use (virtual threads)")
    void isThreadSafe() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<Decision>>();
            IntStream.range(0, 1_000).forEach(i -> {
                // Alternate between a good and a terrible profile; any cross-talk shows up as a mismatch.
                var fact = i % 2 == 0 ? new LoanFact(800, 1.0) : new LoanFact(300, 1.0);
                futures.add(executor.submit(() -> engine.evaluate(fact)));
            });

            for (int i = 0; i < futures.size(); i++) {
                Decision decision = futures.get(i).get();
                Class<? extends Decision> expected =
                        i % 2 == 0 ? Decision.Approved.class : Decision.Rejected.class;
                assertThat(decision).isInstanceOf(expected);
            }
        }
    }
}
