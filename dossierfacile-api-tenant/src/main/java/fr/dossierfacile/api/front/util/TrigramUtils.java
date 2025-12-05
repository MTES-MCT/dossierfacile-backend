package fr.dossierfacile.api.front.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Optional;

public class TrigramUtils {

    private static final int TRIGRAM_LENGTH = 3;

    public static Optional<String> compute(String lastName) {
        if (StringUtils.isBlank(lastName)) {
            return Optional.empty();
        }

        String normalized = StringUtils.stripAccents(lastName)
                .replace("'", "")
                .replace(" ", "")
                .replace("-", "");

        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        
        // Remove all non-letters (ASCII letters only)
        String lettersOnly = normalized.replaceAll("[^A-Za-z]", "");
        if (lettersOnly.isEmpty()) {
            return Optional.empty();
        }

        String trigram = lettersOnly.substring(0, Math.min(TRIGRAM_LENGTH, lettersOnly.length()))
                .toUpperCase(Locale.ROOT);
        return Optional.of(trigram);
    }
}

