package fr.dossierfacile.api.front.exception.controller;

import fr.dossierfacile.api.front.exception.ApplicationRegistrationException;
import fr.dossierfacile.api.front.model.tenant.ApplicationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Highest precedence so this exception is resolved here and never swallowed by the
 * catch-all Exception handler of the common GlobalExceptionHandler (advice order is
 * otherwise unspecified). Only ApplicationRegistrationException is handled, everything
 * else falls through to the other advices.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ApplicationRegistrationExceptionHandler {

    @ExceptionHandler(ApplicationRegistrationException.class)
    public ResponseEntity<ApplicationErrorResponse> handleApplicationRegistrationException(ApplicationRegistrationException ex) {
        log.warn("Application step rejected with code {}: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getCode().httpStatus()).body(new ApplicationErrorResponse(ex.getCode()));
    }
}
