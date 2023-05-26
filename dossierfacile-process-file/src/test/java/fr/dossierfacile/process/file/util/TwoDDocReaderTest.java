package fr.dossierfacile.process.file.util;

import com.google.zxing.NotFoundException;
import fr.dossierfacile.process.file.TestFilesUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TwoDDocReaderTest {

	@Test
	void should_read_2ddoc_on_pdf() throws IOException, NotFoundException {
		String code = TwoDDocReader.find2DDocOn(TestFilesUtil.getPdfBoxDocument("2ddoc.pdf"));
		assertThat(code).isEqualTo("DC02FR000001125E125B0126FR247500010MME/SPECIMEN/NATACHA\u001D22145 AVENUE DES SPECIMENS\u001D\u001F54LDD5F7JD4JEFPR6WZYVZVB2JZXPZB73SP7WUTN5N44P3GESXW75JZUZD5FM3G4URAJ6IKDSSUB66Y3OWQIEH22G46QOAGWH7YHJWQ");
	}

}