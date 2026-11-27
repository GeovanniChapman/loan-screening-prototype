package dev.geo.loanscreening.web;

import dev.geo.loanscreening.service.ApplicationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into RFC 9457 {@code application/problem+json} responses.
 * Validation errors (400) are handled by Spring's built-in ProblemDetail support, enabled with
 * {@code spring.mvc.problemdetails.enabled=true}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException.class)
    ProblemDetail notFound(ApplicationNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
