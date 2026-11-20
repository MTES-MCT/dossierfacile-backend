package fr.dossierfacile.scheduler.tasks.tenantwarning;

/**
 * Thrown when Brevo could not accept an email because of a temporary failure
 * (network error or timeout, rate limiting, server error).
 * Unchecked so that it rolls back the surrounding transaction: the tenant is not marked
 * as warned and the email is sent again on the next run.
 */
public class TransientMailDeliveryException extends RuntimeException {

    private final int httpCode;

    public TransientMailDeliveryException(String message, int httpCode, Throwable cause) {
        super(message, cause);
        this.httpCode = httpCode;
    }

    /**
     * HTTP status returned by Brevo, or 0 when no response was received (network error or timeout).
     */
    public int getHttpCode() {
        return httpCode;
    }
}
