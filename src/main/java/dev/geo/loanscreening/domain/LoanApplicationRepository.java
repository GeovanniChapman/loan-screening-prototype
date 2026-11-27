package dev.geo.loanscreening.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Spring Data repository; the implementation is generated at startup. */
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    /** Newest first. The {@link Pageable} bounds the result size. */
    List<LoanApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Used by the recurring sweeper to find applications whose evaluation job never completed. */
    List<LoanApplication> findByStatusAndCreatedAtBefore(ApplicationStatus status, Instant cutoff);
}
