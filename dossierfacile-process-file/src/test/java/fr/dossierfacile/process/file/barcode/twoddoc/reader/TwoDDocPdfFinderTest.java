package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.TestFilesUtil;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TwoDDocPdfFinderTest {

    @Test
    void should_read_2DDoc_on_pdf() throws IOException {
        PDDocument document = TestFilesUtil.getPdfBoxDocument("2ddoc.pdf");
        Optional<TwoDDocRawContent> actualContent = TwoDDocPdfFinder.on(document).find2DDoc();
        TwoDDocRawContent expectedContent = new TwoDDocRawContent("DC02FR000001125E125B0126FR247500010MME/SPECIMEN/NATACHA\u001D22145 AVENUE DES SPECIMENS\u001D\u001F54LDD5F7JD4JEFPR6WZYVZVB2JZXPZB73SP7WUTN5N44P3GESXW75JZUZD5FM3G4URAJ6IKDSSUB66Y3OWQIEH22G46QOAGWH7YHJWQ");
        assertThat(actualContent).isPresent().contains(expectedContent);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void should_find_2DDoc_on_pdf_regardless_of_text_direction(int page) throws IOException {
        PDDocument document = TestFilesUtil.getPdfBoxDocument("2ddocs.pdf");
        Optional<TwoDDocRawContent> twoDDoc = TwoDDocPdfFinder.on(document, page).find2DDoc();
        assertThat(twoDDoc).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"test-document.pdf", "fake-payfit.pdf"})
    void should_return_empty_if_no_2DDoc_found(String fileName) throws IOException {
        PDDocument document = TestFilesUtil.getPdfBoxDocument(fileName);
        Optional<TwoDDocRawContent> twoDDoc = TwoDDocPdfFinder.on(document).find2DDoc();
        assertThat(twoDDoc).isEmpty();
    }

}