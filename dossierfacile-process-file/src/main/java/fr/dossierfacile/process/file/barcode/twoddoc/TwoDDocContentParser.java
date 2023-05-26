package fr.dossierfacile.process.file.barcode.twoddoc;

public class TwoDDocContentParser {

	private static final char ASCII_GROUP_SEPARATOR = (char)29;
	private static final char ASCII_UNIT_SEPARATOR = (char)31;

	public static TwoDDoc parse(TwoDDocRawContent twoDDocRawContent) {
		String rawContent = twoDDocRawContent.rawContent();
		TwoDDoc twoDDoc = new TwoDDoc();

		if (rawContent == null || rawContent.length() < 26) {
			return twoDDoc;
		}

		int version = Integer.parseInt(rawContent.substring(2,4));
		if (version != 4) {
			return twoDDoc;
		}

		twoDDoc.setIDFlag(rawContent.substring(0,2));
		twoDDoc.setVersion(version);
		twoDDoc.setIssuer(rawContent.substring(4,8));
		twoDDoc.setCertId(rawContent.substring(8, 12));
		twoDDoc.setDocumentDate(rawContent.substring(12, 16));
		twoDDoc.setSignatureDate(rawContent.substring(16, 20));
		twoDDoc.setDocumentTypeId(rawContent.substring(20, 22));
		twoDDoc.setPerimeterId(rawContent.substring(22, 24));
		twoDDoc.setCountryId(rawContent.substring(24, 26));

		String remain = rawContent.substring(26);
		String[] unitParts = remain.split(String.valueOf(ASCII_UNIT_SEPARATOR));
		String data = unitParts[0];
		String signature = unitParts[1];
		twoDDoc.setSignature(signature);

		while (data.length() > 0) {
			int separatorLength = 0;
			String id = data.substring(0, 2);
			data = data.substring(2);
			TwoDDocIdEnum docId = TwoDDocIdEnum.valueOf("ID_"+id);
			if (docId.getMinSize() == docId.getMaxSize()) {
				twoDDoc.getData().put(id, data.substring(0, docId.getMaxSize()));
				data = data.substring(docId.getMaxSize());
			} else {
				String potentialResult = data;
				if (docId.getMaxSize() > 0) {
					potentialResult = data.substring(0, docId.getMaxSize());
				}
				int pos = potentialResult.indexOf(ASCII_GROUP_SEPARATOR);
				if (pos < 0) {
					pos = potentialResult.length();
				} else {
					separatorLength = 1;
				}
				String result = data.substring(0, pos);
				data = data.substring(pos+separatorLength);
				twoDDoc.getData().put(id, result);
			}
		}

		return twoDDoc;
	}

}
