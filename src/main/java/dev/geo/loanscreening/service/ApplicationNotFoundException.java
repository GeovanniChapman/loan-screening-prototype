package dev.geo.loanscreening.service;

import java.util.UUID;

/** Thrown when a loan application id does not exist. Mapped to HTTP 404 by the web layer. */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(UUID id) {
        super("Loan application %s not found".formatted(id));
    }
}
