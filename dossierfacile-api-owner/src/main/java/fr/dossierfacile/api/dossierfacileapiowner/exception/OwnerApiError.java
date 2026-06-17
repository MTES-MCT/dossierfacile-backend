package fr.dossierfacile.api.dossierfacileapiowner.exception;

import java.util.List;
import java.util.Map;

public record OwnerApiError(
        String code,
        String message,
        Map<String, Object> details,
        List<String> errors
) {
    public OwnerApiError(String code, String message) {
        this(code, message, Map.of(), List.of());
    }
}
