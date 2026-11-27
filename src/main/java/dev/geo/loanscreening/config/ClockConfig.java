package dev.geo.loanscreening.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes a {@link Clock} bean. Code asks the injected clock for the time instead of calling
 * {@code Instant.now()}, so tests can substitute a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
