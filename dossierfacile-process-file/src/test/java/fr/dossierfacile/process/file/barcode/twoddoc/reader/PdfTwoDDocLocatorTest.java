package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.TestFilesUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTwoDDocLocatorTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void should_find_potential_2DDoc_location_based_on_text(int page) throws IOException {
        PDDocument document = TestFilesUtil.getPdfBoxDocument("2ddocs.pdf");
        PdfTwoDDocLocator locator = new PdfTwoDDocLocator(new PdfPage(document, page));
        assertThat(locator.getPotentialPositionsBasedOnText()).isNotEmpty().hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void should_return_nothing_when_2DDoc_is_not_found_in_text() throws IOException {
        PDDocument document = TestFilesUtil.getPdfBoxDocument("test-document.pdf");
        PdfTwoDDocLocator locator = new PdfTwoDDocLocator(new PdfPage(document, 0));
        assertThat(locator.getCommonlyKnownPositions()).isEmpty();
    }

    @Test
    void should_return_known_2DDoc_locations_for_image_pdfs() throws IOException {
        PDDocument document = TestFilesUtil.getPdfBoxDocument("2ddoc.pdf");
        PdfTwoDDocLocator locator = new PdfTwoDDocLocator(new PdfPage(document, 0));
        assertThat(locator.getCommonlyKnownPositions()).isNotEmpty().hasSizeGreaterThan(3);
    }

}