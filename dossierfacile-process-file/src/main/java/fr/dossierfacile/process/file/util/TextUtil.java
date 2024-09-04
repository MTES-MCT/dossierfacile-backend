package fr.dossierfacile.process.file.util;

public class TextUtil {
    /**
     * Text can have invisible character when reading the pdf directly.
     * Remove these invisible characters.
     */
    public static String cleanAndTrim(String text) {
        if (text == null)
            return null;
        return text.replaceAll("[^\\p{Print}]", "").trim();
    }
}