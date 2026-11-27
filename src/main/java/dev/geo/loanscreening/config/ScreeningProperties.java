package dev.geo.loanscreening.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Type-safe binding for the {@code loan-screening.*} properties.
 *
 * <p>A record is a natural fit for {@code @ConfigurationProperties}: immutable, with
 * constructor binding by default in Spring Boot 3.
 *
 * @param sweepStaleAfter how long an application may stay PENDING before the recurring
 *                        sweeper re-schedules its evaluation (ISO-8601, e.g. {@code PT2M})
 */
@ConfigurationProperties(prefix = "loan-screening")
public record ScreeningProperties(Duration sweepStaleAfter) {

    public ScreeningProperties {
        if (sweepStaleAfter == null) {
            sweepStaleAfter = Duration.ofMinutes(2);
        }
    }
}
