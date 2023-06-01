package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

import org.apache.commons.codec.binary.Base32;

public record TwoDDocSignature(String rawSignature) {

	byte[] decodeBase32() {
		return new Base32().decode(rawSignature.getBytes());
	}

}
