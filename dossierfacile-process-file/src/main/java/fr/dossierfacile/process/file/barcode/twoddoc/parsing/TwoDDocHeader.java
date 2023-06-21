package fr.dossierfacile.process.file.barcode.twoddoc.parsing;

public record TwoDDocHeader(
		int version,
		String issuer,
		String certId,
		String documentDate,
		String signatureDate,
		String documentTypeId,
		String perimeterId,
		String countryId
) {

	static TwoDDocHeader parse(String rawContent) {
		if (!rawContent.startsWith("DC")) {
			throw new TwoDDocParsingException("2D-Doc content must start with 'DC' marker");
		}

		int version = Integer.parseInt(rawContent.substring(2, 4));
		if (version != 4) {
			throw new TwoDDocParsingException(String.format("Version %d of 2D-Doc is not supported", version));
		}

		if (rawContent.length() < 26) {
			throw new TwoDDocParsingException("2D-Doc V4 with C40 encoding must have a 26 characters header");
		}

		return new TwoDDocHeader(
				version,
				rawContent.substring(4, 8),
				rawContent.substring(8, 12),
				rawContent.substring(12, 16),
				rawContent.substring(16, 20),
				rawContent.substring(20, 22),
				rawContent.substring(22, 24),
				rawContent.substring(24, 26)
		);
	}

}