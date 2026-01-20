package fr.dossierfacile.common.utils;

import javax.annotation.Nullable;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class NameUtil {

    // Pattern pour identifier les caractères diacritiques (les accents)
    private static final Pattern DIACRITICS_AND_FRIENDS = Pattern.compile("\\p{M}");

    // Pattern pour identifier tout ce qui N'EST PAS une lettre (A-Z)
    private static final Pattern NON_ALPHABETIC = Pattern.compile("[^a-zA-Z]");

    public static String normalizeName(String name) {
        if (name == null)
            return null;
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return normalized.replace('-', ' ')
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toUpperCase();
    }


    public static String sanitizeForComparison(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // 1. Remplacements manuels pour les caractères qui ne se décomposent pas avec NFD
        String preprocessed = input
                // Caractères scandinaves
                .replace("ø", "o").replace("Ø", "O")
                .replace("å", "a").replace("Å", "A")
                .replace("æ", "ae").replace("Æ", "AE")
                // Caractères slaves
                .replace("ł", "l").replace("Ł", "L")
                // Caractères allemands
                .replace("ß", "ss")
                // Caractères islandais
                .replace("ð", "d").replace("Ð", "D")
                .replace("þ", "th").replace("Þ", "TH");

        // 2. Décomposition canonique (NFD) : sépare 'é' en 'e' + 'accent aigu'
        String normalized = Normalizer.normalize(preprocessed, Normalizer.Form.NFD);

        // 3. Suppression des marques diacritiques (les accents flottants)
        String withoutAccents = DIACRITICS_AND_FRIENDS.matcher(normalized).replaceAll("");

        // 4. Suppression des caractères spéciaux et mise en majuscule
        // On retire ici les espaces, tirets, etc. pour coller les noms
        return NON_ALPHABETIC.matcher(withoutAccents).replaceAll("").toUpperCase(Locale.ROOT);
    }

    public static boolean isNameMatching(IDocumentIdentity document, IDocumentIdentity extractedIdentity) {
        List<String> documentSanitizedFirstNames = document.getFirstNames().stream()
                .map(NameUtil::sanitizeForComparison)
                .toList();
        String documentSanitizedLastName = sanitizeForComparison(document.getLastName());
        String documentSanitizedPreferredName = document.getPreferredName() != null ?
                sanitizeForComparison(document.getPreferredName()) : null;

        List<String> extractedSanitizedFirstNames = extractedIdentity.getFirstNames().stream()
                .map(NameUtil::sanitizeForComparison)
                .toList();
        String extractedSanitizedLastName = sanitizeForComparison(extractedIdentity.getLastName());
        String extractedSanitizedPreferredName = extractedIdentity.getPreferredName() != null ?
                sanitizeForComparison(extractedIdentity.getPreferredName()) : null;

        boolean hasMatchingFirstName = hasMatchingFirstName(documentSanitizedFirstNames, extractedSanitizedFirstNames);
        boolean hasMatchingLastName = hasMatchingLastName(
                documentSanitizedLastName,
                documentSanitizedPreferredName,
                extractedSanitizedLastName,
                extractedSanitizedPreferredName
        );

        return hasMatchingLastName && hasMatchingFirstName;
    }

    private static boolean hasMatchingFirstName(List<String> documentFirstNames, List<String> extractedFirstNames) {
        // un prénom du document est contenu dans un prénom extrait ou l'inverse
        return documentFirstNames.stream().anyMatch(
                docFirst -> extractedFirstNames.stream()
                        .anyMatch(extFirst -> !docFirst.isEmpty() && !extFirst.isEmpty() &&
                                (docFirst.contains(extFirst) || extFirst.contains(docFirst))
                        )
        ) || extractedFirstNames.stream().anyMatch(
                extFirst -> documentFirstNames.stream()
                        .anyMatch(docFirst -> !docFirst.isEmpty() && !extFirst.isEmpty() &&
                                (docFirst.contains(extFirst) || extFirst.contains(docFirst))
                        )
        );
    }

    private static boolean hasMatchingLastName(String documentLastName,
                                               @Nullable String documentPreferredName,
                                               String extractedLastName,
                                               @Nullable String extractedPreferredName) {
        boolean hasMatchingLastName = documentLastName.contains(extractedLastName)
                || extractedLastName.contains(documentLastName);

        if (documentPreferredName != null &&
                (documentPreferredName.contains(extractedLastName)
                        || (extractedPreferredName != null && documentPreferredName.contains(extractedPreferredName)))) {
            hasMatchingLastName = true;
        }

        if (extractedPreferredName != null &&
                (extractedPreferredName.contains(documentLastName)
                        || (documentPreferredName != null && extractedPreferredName.contains(documentPreferredName)))) {
            hasMatchingLastName = true;
        }

        return hasMatchingLastName;
    }

}
