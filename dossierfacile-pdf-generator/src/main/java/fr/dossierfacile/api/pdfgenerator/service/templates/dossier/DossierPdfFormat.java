package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.common.entity.Tenant;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Formats in French the values that the dossier PDF displays.
 */
final class DossierPdfFormat {

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final String[] COUNT_WORDS = {"Zéro", "Un", "Deux", "Trois", "Quatre", "Cinq", "Six", "Sept", "Huit", "Neuf", "Dix", "Onze", "Douze"};
    // A non-breaking space keeps "9 600 €" on one line. Marianne lacks the narrow one that the French locale uses.
    private static final char NON_BREAKING_SPACE = ' ';

    private DossierPdfFormat() {}

    /** Formats a date in French, for example "6 octobre 2026" or "1er octobre 2026". */
    static String date(LocalDate date) {
        String day = date.getDayOfMonth() == 1 ? "1er" : String.valueOf(date.getDayOfMonth());
        return day + " " + MONTH_YEAR.format(date);
    }

    /** Formats an amount in euros, for example "9 600 €". */
    static String euros(int amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.FRENCH);
        symbols.setGroupingSeparator(NON_BREAKING_SPACE);
        return new DecimalFormat("#,##0", symbols).format(amount) + NON_BREAKING_SPACE + "€";
    }

    /**
     * Puts "de" before a name, or "d'" before a vowel, for example "de Camille" or "d'Anne-Laure". It does not elide
     * before an H, which may be aspirated as in "de Hugo", nor before a Y followed by a vowel, as in "de Yann". It
     * returns an empty text when the name is missing.
     */
    static String of(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        String start = StringUtils.stripAccents(name).toLowerCase(Locale.FRENCH);
        boolean elided = start.matches("[aeiouœæ].*") || start.matches("y[^aeiouy].*");
        return (elided ? "d'" : "de ") + name;
    }

    /**
     * Returns the full name of the candidate, or the part of it that we know. {@link Tenant#getFullName()} returns an
     * empty text as soon as the first name or the last name is missing.
     */
    static String fullName(Tenant tenant) {
        return StringUtils.firstNonBlank(tenant.getFullName(), tenant.getFirstName(), tenant.getPreferredName(), tenant.getLastName(), "");
    }

    /**
     * Removes the characters that XML forbids, such as control characters and unpaired surrogates. A single one, for
     * example in the text that a candidate wrote, makes the engine reject the whole document.
     */
    static String validXml(String text) {
        StringBuilder valid = new StringBuilder(text.length());
        text.codePoints().filter(DossierPdfFormat::isValidXml).forEach(valid::appendCodePoint);
        return valid.toString();
    }

    private static boolean isValidXml(int character) {
        return character == '\t' || character == '\n' || character == '\r'
                || character >= 0x20 && character <= 0xD7FF
                || character >= 0xE000 && character <= 0xFFFD
                || character >= 0x10000;
    }

    /** Joins items the French way, for example "A", "A et B" or "A, B et C". */
    static String enumeration(List<String> items) {
        if (items.size() <= 1) {
            return String.join("", items);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " et " + items.get(items.size() - 1);
    }

    /** Writes a count in letters up to twelve, for example "Une personne", "Trois personnes" or "Un organisme". */
    static String count(int count, String singular, String plural, boolean feminine) {
        String number = count < COUNT_WORDS.length ? COUNT_WORDS[count] : String.valueOf(count);
        if (count == 1 && feminine) {
            number = "Une";
        }
        return number + " " + (count > 1 ? plural : singular);
    }
}
