package fr.dossierfacile.process.file.util;

import java.text.Normalizer;

public class NameUtil {
    public static String normalizeName(String name) {
        if (name == null)
            return null;
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return normalized.replace('-', ' ')
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toUpperCase();
    }

}
