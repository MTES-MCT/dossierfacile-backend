package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PieceExtractorTest {

    private final PieceExtractor extractor = new PieceExtractor();

    @Test
    void should_reuse_the_jpeg_of_a_page_as_it_is() throws IOException {
        byte[] pdf = resource("/page1.pdf");

        assertThat(extract(pdf)).singleElement().satisfies(page -> {
            assertThat(page.isImage()).isTrue();
            assertThat(page.file()).isEqualTo(jpegOf(pdf));
            assertThat(page.width()).isEqualTo(896);
            assertThat(page.height()).isEqualTo(1364);
            assertThat(page.text()).isEmpty();
        });
    }

    @Test
    void should_keep_the_other_pages_as_pdf_pages_with_their_text() throws IOException {
        List<PieceContent> pages = extract(TestPdfs.statement("Première page", "Deuxième  page"));

        assertThat(pages).extracting(PieceContent::pdfPage).containsExactly(1, 2);
        assertThat(pages).extracting(PieceContent::text).containsExactly("Première page", "Deuxième page");
        assertThat(pages.get(0).file()).isSameAs(pages.get(1).file());
        assertThat(pages.get(0).width()).isEqualTo(PDRectangle.A4.getWidth());
    }

    @Test
    void should_straighten_a_rotated_page() throws IOException {
        byte[] statement = TestPdfs.statement("Première page", "Deuxième page");

        for (int rotation : new int[]{90, 180, 270}) {
            byte[] rotated = TestPdfs.rotated(statement, rotation);
            List<PieceContent> pages = extract(rotated);

            assertThat(pages).extracting(PieceContent::text).hasSize(2).allSatisfy(text -> assertThat(text).contains("page"));
            boolean sideways = rotation != 180;
            assertThat(pages.get(1).width()).isCloseTo(sideways ? PDRectangle.A4.getHeight() : PDRectangle.A4.getWidth(), within(0.1f));
            // The new page has no rotation and displays like the rotated one.
            try (PDDocument expected = Loader.loadPDF(rotated); PDDocument actual = Loader.loadPDF(pages.get(1).file())) {
                assertThat(actual.getPage(0).getRotation()).isZero();
                assertThat(pages.get(1).pdfPage()).isEqualTo(1);
                BufferedImage expectedImage = new PDFRenderer(expected).renderImage(1);
                BufferedImage actualImage = new PDFRenderer(actual).renderImage(0);
                assertThat(actualImage.getWidth()).isEqualTo(expectedImage.getWidth());
                assertThat(actualImage.getHeight()).isEqualTo(expectedImage.getHeight());
                assertThat(missingText(expectedImage, actualImage)).isLessThan(0.02);
                assertThat(missingText(actualImage, expectedImage)).isLessThan(0.02);
            }
        }
        assertThat(extract(TestPdfs.rotated(resource("/page1.pdf"), 90)).get(0).isImage()).isFalse();
    }

    @Test
    void should_keep_a_transparent_image_as_a_pdf_page() throws IOException {
        assertThat(extract(resource("/landscapeid.pdf"))).singleElement().satisfies(page -> {
            assertThat(page.isImage()).isFalse();
            assertThat(page.width()).isGreaterThan(page.height());
            assertThat(page.text()).isEmpty();
        });
    }

    /** Returns the share of the dark pixels of the expected image that the other image lacks. White pixels do not count. */
    private static double missingText(BufferedImage expected, BufferedImage actual) {
        int text = 0;
        int missing = 0;
        for (int x = 1; x < expected.getWidth() - 1; x++) {
            for (int y = 1; y < expected.getHeight() - 1; y++) {
                if (isDark(expected, x, y)) {
                    text++;
                    // Accept one pixel of difference, because the renderer rounds the positions.
                    boolean found = false;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            found |= isDark(actual, x + dx, y + dy);
                        }
                    }
                    missing += found ? 0 : 1;
                }
            }
        }
        assertThat(text).isPositive();
        return (double) missing / text;
    }

    private static boolean isDark(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0xFF) < 128;
    }

    private List<PieceContent> extract(byte[] pdf) throws IOException {
        return extractor.extract(new ByteArrayInputStream(pdf));
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream stream = PieceExtractorTest.class.getResourceAsStream(name)) {
            return stream.readAllBytes();
        }
    }

    private static byte[] jpegOf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDResources resources = document.getPage(0).getResources();
            PDImageXObject image = (PDImageXObject) resources.getXObject(resources.getXObjectNames().iterator().next());
            try (InputStream jpeg = image.getStream().createInputStream(List.of(COSName.DCT_DECODE.getName()))) {
                return jpeg.readAllBytes();
            }
        }
    }
}
