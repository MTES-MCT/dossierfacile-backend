package fr.dossierfacile.common.exceptions;

public class AdemeApiUnauthorizedException extends RuntimeException {
    public AdemeApiUnauthorizedException(int statusCode, String message) {
        super("ADEME API Unauthorized Error: status code " + statusCode + ", message " + message);
    }
}
