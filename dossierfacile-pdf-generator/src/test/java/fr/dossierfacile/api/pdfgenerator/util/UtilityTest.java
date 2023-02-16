package fr.dossierfacile.api.pdfgenerator.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityTest {

    private static final Resource FONT_SPECTRAL_ITALIC = new ClassPathResource("static/fonts/Spectral-Italic.ttf");
    public static final Resource FONT_NOTO = new ClassPathResource("static/fonts/Noto/NotoEmoji-Medium.ttf");

    @Test
    void testFontSupported() {
        try (PDDocument doc = new PDDocument())
        {
            PDType0Font alternativeFont = PDType0Font.load(doc, FONT_NOTO.getInputStream());
            PDType0Font font = PDType0Font.load(doc, FONT_SPECTRAL_ITALIC.getInputStream());
            assertTrue(Utility.isFontSupported("\uD83D\uDE42", alternativeFont));
            assertFalse(Utility.isFontSupported("\uD83D\uDE42", font));
            assertTrue(Utility.isFontSupported("A", font));
        } catch (Exception e) {
            assert false;
        }
    }

    @Test
    void testSentanceToLines() {
        String l = """
                anruise tanruiset anruise tanrusiet anruiset anrusiet anruset nrauist rste aurnsiet anrusiet anrsa ute narusix rauia anuxrisa aurnanruise tanruiset anruise tanrusiet anruiset anrusiet anruset nrauist rste aurnsiet anrusiet anrsa ute narusix rauia anuxrisa aurn
                """;
        try (PDDocument doc = new PDDocument())
        {
            PDType0Font alternativeFont = PDType0Font.load(doc, FONT_NOTO.getInputStream());
            PDType0Font font = PDType0Font.load(doc, FONT_SPECTRAL_ITALIC.getInputStream());
            assertEquals(2, Utility.sentanceToLines(l, 1000, font, 18, alternativeFont ).size());
        } catch (Exception e) {
            assert false;
        }

    }
}