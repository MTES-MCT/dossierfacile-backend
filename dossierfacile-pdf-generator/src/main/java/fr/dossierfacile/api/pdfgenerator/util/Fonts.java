package fr.dossierfacile.api.pdfgenerator.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public enum Fonts {

    MARIANNE_LIGHT("Marianne-Light.ttf"),
    MARIANNE_REGULAR("Marianne-Regular.ttf"),
    MARIANNE_BOLD("Marianne-Bold.ttf"),
    SPECTRAL_BOLD("Spectral-Bold.ttf"),
    SPECTRAL_EXTRA_BOLD("Spectral-ExtraBold.ttf"),
    SPECTRAL_ITALIC("Spectral-Italic.ttf"),
    ARIAL_NOVA_LIGHT("ArialNova-Light.ttf"),
    NOTO_EMOJI_MEDIUM("Noto/NotoEmoji-Medium.ttf")
    ;

    private static final Logger log = LoggerFactory.getLogger(Fonts.class);
    private final String path;

    Fonts(String path) {
        this.path = path;
    }

    public PDType0Font load(PDDocument document) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("static/fonts/" + path)) {
            if (is == null) {
                throw new IOException("Font file not found: " + path);
            }
            return PDType0Font.load(document, is);
        } catch (Exception e) {
            log.error("Error loading font", e);
            return null;
        }
    }

}
