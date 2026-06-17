package fr.dossierfacile.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AdemeApiErrorMessageParser {

    private AdemeApiErrorMessageParser() {
    }

    public static String extractUserMessage(String responseBody, ObjectMapper objectMapper) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode messageNode = root.get("message");
            if (messageNode != null && messageNode.isTextual() && !messageNode.asText().isBlank()) {
                return messageNode.asText();
            }
        } catch (Exception ignored) {
            // Fall back to raw body below.
        }
        return responseBody;
    }
}
