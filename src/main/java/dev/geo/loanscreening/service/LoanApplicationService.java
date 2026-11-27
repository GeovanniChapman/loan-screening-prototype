package dev.geo.loanscreening.service;

import dev.geo.loanscreening.domain.ApplicationStatus;
import dev.geo.loanscreening.domain.Decision;
import dev.geo.loanscreening.domain.LoanApplication;
import dev.geo.loanscreening.domain.LoanApplicationRepository;
import dev.geo.loanscreening.rules.LoanFact;
import dev.geo.loanscreening.rules.LoanRiskRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Application use cases: submit, query and evaluate loan applications. */
@Service
public class LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationService.class);

    /** Upper bound for list queries, protecting the database from huge page requests. */
    static final int MAX_PAGE_SIZE = 100;

    private final LoanApplicationRepository repository;
    private final LoanRiskRuleEngine ruleEngine;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    // Constructor injection (single constructor => no @Autowired needed): fields are final
    // and the class is trivially unit-testable with plain mocks.
    public LoanApplicationService(LoanApplicationRepository repository, LoanRiskRuleEngine ruleEngine,
                                  ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.ruleEngine = ruleEngine;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Stores a new application as PENDING and announces it. The evaluation itself happens
     * later, in a background job: the HTTP caller never waits for the rules.
     */
    @Transactional
    public LoanApplication submit(SubmitLoanCommand command) {
        var application = LoanApplication.submit(command.applicantName(), command.annualIncome(),
                command.requestedAmount(), command.creditScore(), clock.instant());
        repository.save(application);
        events.publishEvent(new LoanApplicationSubmitted(application.getId()));
        log.info("Loan application {} submitted", application.getId());
        return application;
    }

    @Transactional(readOnly = true)
    public LoanApplication get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    /** Newest applications first. {@code limit} is clamped to {@code [1, MAX_PAGE_SIZE]}. */
    @Transactional(readOnly = true)
    public List<LoanApplication> recent(int limit) {
        // Math.clamp(long, int, int) was added in Java 21.
        int safeLimit = Math.clamp(limit, 1, MAX_PAGE_SIZE);
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.ofSize(safeLimit));
    }

    /**
     * Runs the rules for one application and persists the verdict. Called by the JobRunr job.
     *
     * <p><b>Idempotent</b>: JobRunr guarantees <em>at-least-once</em> execution, so an already
     * decided application is skipped instead of failing.
     *
     * @throws ApplicationNotFoundException if the id is unknown (JobRunr will retry the job)
     */
    @Transactional
    public void evaluate(UUID id) {
        LoanApplication application = get(id);
        if (application.getStatus() != ApplicationStatus.PENDING) {
            log.info("Application {} already {}; nothing to do", id, application.getStatus());
            return;
        }
        Decision decision = ruleEngine.evaluate(LoanFact.from(application));
        application.applyDecision(decision, clock.instant());
        log.info("Application {} -> {}", id, decision.summary());
    }
}
