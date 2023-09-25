package fr.dossierfacile.api.pdfgenerator.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityTest {

    @Test
    void testFontSupported() {
        try (PDDocument doc = new PDDocument())
        {
            PDType0Font alternativeFont = Fonts.NOTO_EMOJI_MEDIUM.load(doc);
            PDType0Font font = Fonts.SPECTRAL_ITALIC.load(doc);
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
            PDType0Font alternativeFont = Fonts.NOTO_EMOJI_MEDIUM.load(doc);
            PDType0Font font = Fonts.SPECTRAL_ITALIC.load(doc);
            assertEquals(2, Utility.sentanceToLines(l, 1000, font, 18, alternativeFont ).size());
        } catch (Exception e) {
            assert false;
        }

    }
}