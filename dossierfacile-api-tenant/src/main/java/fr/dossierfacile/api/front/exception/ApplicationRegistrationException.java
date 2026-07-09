package fr.dossierfacile.api.front.exception;

import fr.dossierfacile.api.front.exception.model.ApplicationErrorCode;
import lombok.Getter;

/**
 * Signals a business-rule violation on the tenant application step. Carries the
 * machine-readable {@link ApplicationErrorCode} that identifies the violation, and optionally a human-readable
 */
@Getter
public class ApplicationRegistrationException extends RuntimeException {

    private final ApplicationErrorCode code;

    public ApplicationRegistrationException(ApplicationErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public ApplicationRegistrationException(ApplicationErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
