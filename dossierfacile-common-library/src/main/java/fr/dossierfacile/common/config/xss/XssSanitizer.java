package fr.dossierfacile.common.config.xss;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

public class XssSanitizer {

    private XssSanitizer() {
        // Private constructor for utility class
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String unescaped = Parser.unescapeEntities(value, false);
        String clean = Jsoup.clean(unescaped, Safelist.none());
        return clean.replace("&amp;", "&")
                    .replace("&#39;", "'")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'");
    }
}
