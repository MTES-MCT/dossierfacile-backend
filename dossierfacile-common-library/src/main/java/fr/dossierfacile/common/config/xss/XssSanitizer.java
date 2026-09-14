package fr.dossierfacile.common.config.xss;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

public class XssSanitizer {

    private XssSanitizer() {
        // Private constructor for utility class
    }

    private static final Safelist HTML_SAFELIST = Safelist.basic()
            .preserveRelativeLinks(true);

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

    public static String cleanHtml(String value) {
        if (value == null) {
            return null;
        }
        return Jsoup.clean(value, HTML_SAFELIST);
    }
}
