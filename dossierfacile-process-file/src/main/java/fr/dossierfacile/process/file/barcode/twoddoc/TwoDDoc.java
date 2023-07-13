package fr.dossierfacile.process.file.barcode.twoddoc;

import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocData;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocHeader;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocSignature;
import fr.dossierfacile.process.file.barcode.twoddoc.validation.SignatureAlgorithm;

import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

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

    public BarCodeDocumentType getDocumentType() {
        return switch (header.certId()) {
            case "FPE3" -> BarCodeDocumentType.TAX_ASSESSMENT;
            case "FPE4" -> BarCodeDocumentType.TAX_DECLARATION;
            case "SNC2", "SNC3" -> BarCodeDocumentType.SNCF_PAYSLIP;
            case "FPE2" -> BarCodeDocumentType.PUBLIC_PAYSLIP;
            case "FRE0" -> BarCodeDocumentType.FREE_INVOICE;
            case "CNO3" -> BarCodeDocumentType.CVEC;
            default -> BarCodeDocumentType.UNKNOWN;
        };
    }

    public boolean isSignedBy(X509Certificate signingCertificate) throws InvalidKeyException, SignatureException {
        Signature signatureVerifier = SignatureAlgorithm.of(signingCertificate).getInstance();
        signatureVerifier.initVerify(signingCertificate.getPublicKey());
        signatureVerifier.update(rawSignedMessage.getBytes());
        return signatureVerifier.verify(this.signature.encodeDer());
    }

}
