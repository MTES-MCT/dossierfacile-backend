package fr.dossierfacile.process.file.barcode.twoddoc;

import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocData;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocHeader;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocSignature;

import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_42;
import static fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType.ID_47;

public record TwoDDoc(
        TwoDDocHeader header,
        TwoDDocData data,
        TwoDDocSignature signature,
        String annexe,
        String rawSignedMessage
) {

    public String getFiscalNumber() {
        return data.get(ID_47);
    }

    public String getReferenceNumber() {
        return data.get(ID_42);
    }

    public byte[] getSignedBytes() {
        return rawSignedMessage.getBytes();
    }

}
