package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.TestFilesUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextExtractorTest {

    @Test
    void should_find_given_text_on_pdf() throws IOException {
        PdfTextExtractor textLocator = getTextLocatorFor("test-document.pdf");
        List<TextPosition> positions = textLocator.findText("Test");

        assertThat(positions).isNotEmpty().hasSize(1);

        Coordinates coordinates = Coordinates.of(positions.get(0));
        assertThat(coordinates).isEqualTo(new Coordinates(86, 66));
    }

    @Test
    void should_find_nothing() throws IOException {
        PdfTextExtractor textLocator = getTextLocatorFor("test-document.pdf");
        List<TextPosition> positions = textLocator.findText("2D-DOC");

        assertThat(positions).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "2ddoc.pdf, ''",
            "test-document.pdf, Test document"
    })
    void should_extract_document_text(String fileName, String expectedText) throws IOException {
        PdfTextExtractor pdfTextLocator = getTextLocatorFor(fileName);

        assertThat(pdfTextLocator.getDocumentText()).isEqualToIgnoringWhitespace(expectedText);
    }

    private static PdfTextExtractor getTextLocatorFor(String fileName) throws IOException {
        PDDocument document = TestFilesUtil.getPdfBoxDocument(fileName);
        return new PdfTextExtractor(new PdfPage(document, 0));
    }

}