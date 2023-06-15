package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TwoDDocHeaderTest {

	@Test
	void assert_2ddoc_always_start_with_DC() {
		assertThatThrownBy(() -> TwoDDocHeader.parse("ABC"))
				.isInstanceOf(TwoDDocParsingException.class)
				.hasMessage("2D-Doc content must start with 'DC' marker");
	}

	@Test
	void only_support_2ddoc_v4() {
		assertThatThrownBy(() -> TwoDDocHeader.parse("DC03xxxx"))
				.isInstanceOf(TwoDDocParsingException.class)
				.hasMessage("Version 3 of 2D-Doc is not supported");
	}

	@Test
	void header_should_have_26_caracters() {
		assertThatThrownBy(() -> TwoDDocHeader.parse("DC04xxxx"))
				.isInstanceOf(TwoDDocParsingException.class)
				.hasMessage("2D-Doc V4 with C40 encoding must have a 26 characters header");
	}

	@Test
	void should_parse_2ddoc_header_content() {
		TwoDDocHeader actualHeader = TwoDDocHeader.parse("DC04FR04FPE3FFFF202A0401FR");
		TwoDDocHeader expectedHeader = new TwoDDocHeader(4, "FR04", "FPE3", "FFFF", "202A", "04", "01", "FR");
		assertThat(actualHeader).isEqualTo(expectedHeader);
	}

}