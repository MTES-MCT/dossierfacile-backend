package fr.dossierfacile.api.pdfgenerator.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;

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

    private final String path;

    Fonts(String path) {
        this.path = path;
    }

    public PDType0Font load(PDDocument document) throws IOException {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:static/fonts/" + path);
        return PDType0Font.load(document, resource.getInputStream());
    }

}
