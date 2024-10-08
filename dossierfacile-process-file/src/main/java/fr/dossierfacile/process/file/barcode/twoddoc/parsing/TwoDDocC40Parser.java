package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDoc;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;

public class TwoDDocC40Parser {

	public static final char ASCII_GROUP_SEPARATOR = (char)29;
	private static final char ASCII_UNIT_SEPARATOR = (char)31;

	public static TwoDDoc parse(TwoDDocRawContent twoDDocRawContent) {
		String rawContent = twoDDocRawContent.rawContent();

		String rawHeader = rawContent.substring(0, 26);
		TwoDDocHeader header = TwoDDocHeader.parse(rawHeader);

		String[] parts = rawContent.substring(26).split(String.valueOf(ASCII_UNIT_SEPARATOR));
		String rawData = parts[0];
		TwoDDocData data = TwoDDocData.parse(rawData);

		if (parts.length == 1) {
			throw new TwoDDocParsingException("No signature found on 2D-Doc");
		}

		String[] signatureAndAnnexe = parts[1].split(String.valueOf(ASCII_GROUP_SEPARATOR));
		TwoDDocSignature signature = TwoDDocSignature.decodeBase32(signatureAndAnnexe[0]);
		String annexe = signatureAndAnnexe.length > 1 ? signatureAndAnnexe[1] : "";

		return new TwoDDoc(header, data, signature, annexe, rawHeader + rawData);
	}

}
