package dev.geo.loanscreening.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root: a request from a person for a loan.
 *
 * <p>The schema is created by Liquibase (see {@code db/changelog}); Hibernate only
 * <em>validates</em> it ({@code ddl-auto=validate}).
 *
 * <p>State changes go through domain methods ({@link #submit}, {@link #applyDecision}) rather
 * than public setters, so invariants live in one place.
 */
@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    private UUID id;

    /**
     * Optimistic-locking counter. It also lets Spring Data tell a brand-new entity (version is
     * {@code null}) from an existing one, so {@code save} issues a plain INSERT.
     */
    @Version
    private Long version;

    @Column(name = "applicant_name", nullable = false, length = 120)
    private String applicantName;

    @Column(name = "annual_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "credit_score", nullable = false)
    private int creditScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "decision_reason", length = 255)
    private String decisionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    /** Required by JPA. Not for application code. */
    protected LoanApplication() {
    }

    private LoanApplication(String applicantName, BigDecimal annualIncome,
                            BigDecimal requestedAmount, int creditScore, Instant now) {
        this.id = UUID.randomUUID();
        this.applicantName = applicantName;
        this.annualIncome = annualIncome;
        this.requestedAmount = requestedAmount;
        this.creditScore = creditScore;
        this.status = ApplicationStatus.PENDING;
        this.createdAt = now;
    }

    /**
     * Factory for a new application in {@link ApplicationStatus#PENDING} state.
     *
     * @param now creation time, injected (from a {@link java.time.Clock}) to keep this testable
     * @throws IllegalArgumentException if the figures are not strictly positive
     */
    public static LoanApplication submit(String applicantName, BigDecimal annualIncome,
                                         BigDecimal requestedAmount, int creditScore, Instant now) {
        Objects.requireNonNull(applicantName, "applicantName");
        Objects.requireNonNull(now, "now");
        if (annualIncome == null || annualIncome.signum() <= 0) {
            throw new IllegalArgumentException("annualIncome must be positive");
        }
        if (requestedAmount == null || requestedAmount.signum() <= 0) {
            throw new IllegalArgumentException("requestedAmount must be positive");
        }
        return new LoanApplication(applicantName, annualIncome, requestedAmount, creditScore, now);
    }

    /**
     * Records the rules' verdict. Allowed exactly once: a decided application is immutable,
     * which makes the background job safe to retry.
     *
     * @throws IllegalStateException if a decision was already applied
     */
    public void applyDecision(Decision decision, Instant now) {
        if (status.isFinal()) {
            throw new IllegalStateException(
                    "Application %s was already decided: %s".formatted(id, status));
        }
        this.status = decision.status();
        this.decisionReason = decision.reason();
        this.evaluatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }
}
