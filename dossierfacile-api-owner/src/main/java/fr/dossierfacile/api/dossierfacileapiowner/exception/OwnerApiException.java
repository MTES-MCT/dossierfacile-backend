package fr.dossierfacile.api.dossierfacileapiowner.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class OwnerApiException extends RuntimeException {

    private final OwnerApiErrorCode code;
    private final Map<String, Object> details;
    private final List<String> errors;

    public OwnerApiException(OwnerApiErrorCode code, String message) {
        this(code, message, Map.of(), List.of());
    }

    public OwnerApiException(OwnerApiErrorCode code, String message, Map<String, Object> details) {
        this(code, message, details, List.of());
    }

    public OwnerApiException(OwnerApiErrorCode code, String message, List<String> errors) {
        this(code, message, Map.of(), errors);
    }

    public OwnerApiException(OwnerApiErrorCode code, String message, Map<String, Object> details, List<String> errors) {
        super(message);
        this.code = code;
        this.details = details;
        this.errors = errors != null ? errors : List.of();
    }
}
