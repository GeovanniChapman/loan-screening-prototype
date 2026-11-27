package dev.geo.loanscreening;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point.
 *
 * <p>Big picture:
 * <pre>
 *   POST /api/applications ──► save (PENDING) ──► [after commit] enqueue JobRunr job
 *                                                      │
 *                          Drools rules ◄── LoanEvaluationJob (background worker)
 *                                │
 *                                └──► status = APPROVED | MANUAL_REVIEW | REJECTED
 * </pre>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LoanScreeningApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanScreeningApplication.class, args);
    }
}
