package dev.geo.loanscreening.jobs;

import dev.geo.loanscreening.service.LoanApplicationSubmitted;
import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Enqueues {@link LoanEvaluationJob}s. The only place that talks to {@link JobScheduler}. */
@Component
public class LoanEvaluationScheduler {

    private static final Logger log = LoggerFactory.getLogger(LoanEvaluationScheduler.class);

    private final JobScheduler jobScheduler;

    public LoanEvaluationScheduler(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    /**
     * Fires only <b>after the submit transaction committed</b>. Enqueuing inside the transaction
     * could let a fast worker look for a row that is not visible yet.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(LoanApplicationSubmitted event) {
        schedule(event.applicationId());
    }

    /**
     * Enqueues an evaluation. The job id is derived from the application id, so scheduling the
     * same application twice (e.g. by the sweeper) never creates a duplicate job.
     */
    public void schedule(UUID applicationId) {
        // IocJobLambda form: the lambda parameter is the Spring bean, resolved by the worker.
        jobScheduler.<LoanEvaluationJob>enqueue(jobIdFor(applicationId), job -> job.run(applicationId));
        log.debug("Evaluation job enqueued for application {}", applicationId);
    }

    /** Deterministic (name-based, version 3) UUID: same application, same job id. */
    static UUID jobIdFor(UUID applicationId) {
        return UUID.nameUUIDFromBytes(("evaluate-loan-" + applicationId).getBytes(StandardCharsets.UTF_8));
    }
}
