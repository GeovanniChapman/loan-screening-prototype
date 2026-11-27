package dev.geo.loanscreening.jobs;

import dev.geo.loanscreening.config.ScreeningProperties;
import dev.geo.loanscreening.domain.ApplicationStatus;
import dev.geo.loanscreening.domain.LoanApplication;
import dev.geo.loanscreening.domain.LoanApplicationRepository;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Safety net: a <b>recurring</b> JobRunr job that re-schedules applications stuck in PENDING
 * (for example because the process crashed between the commit and the enqueue).
 *
 * <p>{@code @Recurring} registers the job automatically at startup. The cron expression comes
 * from configuration ({@code loan-screening.sweep.cron}) so tests and environments can change it.
 */
@Component
public class PendingApplicationSweeper {

    private static final Logger log = LoggerFactory.getLogger(PendingApplicationSweeper.class);

    private final LoanApplicationRepository repository;
    private final LoanEvaluationScheduler scheduler;
    private final ScreeningProperties properties;
    private final Clock clock;

    public PendingApplicationSweeper(LoanApplicationRepository repository, LoanEvaluationScheduler scheduler,
                                     ScreeningProperties properties, Clock clock) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.properties = properties;
        this.clock = clock;
    }

    @Recurring(id = "sweep-stale-pending-applications", cron = "${loan-screening.sweep.cron}")
    @Job(name = "Sweep stale PENDING loan applications")
    public void sweep() {
        Instant cutoff = clock.instant().minus(properties.sweepStaleAfter());
        List<LoanApplication> stale = repository.findByStatusAndCreatedAtBefore(ApplicationStatus.PENDING, cutoff);
        if (stale.isEmpty()) {
            return;
        }
        log.warn("Re-scheduling {} stale PENDING application(s)", stale.size());
        stale.forEach(application -> scheduler.schedule(application.getId()));
    }
}
