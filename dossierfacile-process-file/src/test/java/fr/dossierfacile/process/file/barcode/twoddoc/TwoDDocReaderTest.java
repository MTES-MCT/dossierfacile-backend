package fr.dossierfacile.process.file.barcode.twoddoc;

import fr.dossierfacile.process.file.TestFilesUtil;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TwoDDocReaderTest {

	@Test
	void should_read_2ddoc_on_pdf() throws IOException {
		PDDocument document = TestFilesUtil.getPdfBoxDocument("2ddoc.pdf");
		Optional<TwoDDocRawContent> actualContent = TwoDDocReader.find2DDocOn(document);
		TwoDDocRawContent expectedContent = new TwoDDocRawContent("DC02FR000001125E125B0126FR247500010MME/SPECIMEN/NATACHA\u001D22145 AVENUE DES SPECIMENS\u001D\u001F54LDD5F7JD4JEFPR6WZYVZVB2JZXPZB73SP7WUTN5N44P3GESXW75JZUZD5FM3G4URAJ6IKDSSUB66Y3OWQIEH22G46QOAGWH7YHJWQ");
		assertThat(actualContent).isPresent().contains(expectedContent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"test-document.pdf", "fake-payfit.pdf"})
	void should_return_empty_if_no_2ddoc_found(String fileName) throws IOException {
		PDDocument document = TestFilesUtil.getPdfBoxDocument(fileName);
		assertThat(TwoDDocReader.find2DDocOn(document)).isEmpty();
	}

}