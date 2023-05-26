package fr.dossierfacile.process.file.barcode.twoddoc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TwoDDocContentParserTest {

	@Test
	void should_parse_2ddoc_raw_content() {
		TwoDDocRawContent rawContent = new TwoDDocRawContent("DC04FR04FPE3FFFF202A0401FR431,5\u001D444237A1861133245202146DOE JOHN\u001D4A310420224712670275544994142000\u001D\u001FC27GM6DXHV2PWAIZ5Q25SBOC64EH6O3IQWWYADIV3YH7ZKJ3JHACHP5EZLBZ6GP6SDE6ZYKCZHKYRRFAJ5NSV5YKO5MVGPTQDEPSZ3Y");
		TwoDDoc twoDDoc = TwoDDocContentParser.parse(rawContent);

		assertThat(twoDDoc.getVersion()).isEqualTo(4);
		assertThat(twoDDoc.getFiscalNumber()).isEqualTo("1267027554499");
		assertThat(twoDDoc.getSignature()).isEqualTo("C27GM6DXHV2PWAIZ5Q25SBOC64EH6O3IQWWYADIV3YH7ZKJ3JHACHP5EZLBZ6GP6SDE6ZYKCZHKYRRFAJ5NSV5YKO5MVGPTQDEPSZ3Y");
	}

}