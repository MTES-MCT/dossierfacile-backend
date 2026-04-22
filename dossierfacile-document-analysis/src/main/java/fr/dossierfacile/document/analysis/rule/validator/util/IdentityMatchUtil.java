package fr.dossierfacile.document.analysis.rule.validator.util;

import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;
import fr.dossierfacile.document.analysis.rule.validator.french_identity_card.document_ia_model.DocumentIdentity;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public final class IdentityMatchUtil {

    private static final int LAST_NAME_MAX_DISTANCE = 2;
    private static final int MIN_LENGTH_FOR_LEVENSHTEIN = 4;
    private static final String NAME_SEPARATOR_REGEX = "[-'_]";

    private IdentityMatchUtil() {
    }

    public static boolean hasFirstNameMatch(List<String> identities, DocumentIdentity documentIdentity) {
        if (documentIdentity == null) {
            return false;
        }

        List<String> firstNamesToMatch = documentIdentity.getFirstNames().stream()
                .map(IdentityMatchUtil::normalize)
                .filter(name -> !name.isBlank())
                .flatMap(name -> Stream.of(name, name.replaceAll(NAME_SEPARATOR_REGEX, " ")))
                .flatMap(IdentityMatchUtil::splitTokens)
                .distinct()
                .toList();

        return hasIdentityMatch(identities, firstNamesToMatch, false);
    }

    public static boolean hasLastNameMatch(List<String> identities, DocumentIdentity documentIdentity) {
        if (documentIdentity == null) {
            return false;
        }

        List<String> lastNamesToMatch = Stream.of(documentIdentity.getLastName(), documentIdentity.getPreferredName())
                .map(IdentityMatchUtil::normalize)
                .filter(name -> !name.isBlank())
                .flatMap(IdentityMatchUtil::lastNameVariants)
                .distinct()
                .toList();

        return hasIdentityMatch(identities, lastNamesToMatch, true);
    }

    public static boolean hasIdentityMatch(List<String> identities, List<String> expectedTokens, boolean strictOnWholeIdentity) {
        if (identities == null || identities.isEmpty() || expectedTokens.isEmpty()) {
            return false;
        }

        List<String> normalizedIdentities = identities.stream()
                .map(IdentityMatchUtil::normalize)
                .filter(identity -> !identity.isBlank())
                .toList();

        if (normalizedIdentities.isEmpty()) {
            return false;
        }

        boolean strictMatch;
        if (strictOnWholeIdentity) {
            strictMatch = normalizedIdentities.stream()
                    .anyMatch(identity -> expectedTokens.stream().anyMatch(identity::contains));
        } else {
            strictMatch = normalizedIdentities.stream()
                    .flatMap(IdentityMatchUtil::splitTokens)
                    .anyMatch(token -> expectedTokens.stream().anyMatch(token::contains));
        }

        if (strictMatch) {
            return true;
        }

        LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
        return normalizedIdentities.stream()
                .flatMap(IdentityMatchUtil::splitTokens)
                .anyMatch(token -> expectedTokens.stream()
                        .filter(expectedToken -> token.length() >= MIN_LENGTH_FOR_LEVENSHTEIN
                                && expectedToken.length() >= MIN_LENGTH_FOR_LEVENSHTEIN)
                        .anyMatch(expectedToken -> {
                            Integer distance = levenshtein.apply(token, expectedToken);
                            return distance != null && distance <= LAST_NAME_MAX_DISTANCE;
                        }));
    }

    public static Stream<String> splitTokens(String identity) {
        return BaseDocumentIAValidator.TOKEN_SEPARATOR.splitAsStream(identity)
                .filter(Objects::nonNull)
                .filter(token -> !token.isBlank());
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String noAccent = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return noAccent.trim().toUpperCase(Locale.ROOT);
    }

    private static Stream<String> lastNameVariants(String normalizedName) {
        Stream<String> baseVariants = Stream.of(
                normalizedName,
                normalizedName.replaceAll(NAME_SEPARATOR_REGEX, " ")
        );

        Stream<String> composedVariant = Stream.empty();
        List<String> tokens = splitTokens(normalizedName).toList();

        if (tokens.size() >= 2) {
            composedVariant = tokens.stream()
                    .filter(token -> token.length() >= MIN_LENGTH_FOR_LEVENSHTEIN)
                    .map(token -> token.replaceAll(NAME_SEPARATOR_REGEX, " "));
        }

        return Stream.concat(baseVariants, composedVariant);
    }
}
