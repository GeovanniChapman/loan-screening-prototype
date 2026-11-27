package dev.geo.loanscreening.jobs;

import dev.geo.loanscreening.service.LoanApplicationService;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The unit of work executed by a JobRunr background worker.
 *
 * <p>JobRunr serializes the <em>method call</em> (bean type, method, arguments) into its storage;
 * a worker, possibly in another JVM, deserializes it and calls the Spring bean. Hence the
 * arguments should be small and serializable: we pass the id, never the entity.
 */
@Component
public class LoanEvaluationJob {

    private final LoanApplicationService service;

    public LoanEvaluationJob(LoanApplicationService service) {
        this.service = service;
    }

    /**
     * {@code %0} in the name is replaced by the first argument, so the dashboard shows
     * "Evaluate loan application 5f2c...". {@code retries = 3} overrides JobRunr's default (10).
     */
    @Job(name = "Evaluate loan application %0", retries = 3)
    public void run(UUID applicationId) {
        service.evaluate(applicationId);
    }
}
