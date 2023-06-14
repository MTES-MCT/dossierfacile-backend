package fr.dossierfacile.process.file.barcode.twoddoc.validation;

import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDoc;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

@Service
@AllArgsConstructor
public class TwoDDocValidator {

    private final TwoDDocCertificationAuthorities certificationAuthorities;

    public boolean checkValidity(TwoDDoc twoDDoc) {
        X509Certificate signingCertificate = certificationAuthorities.getCertificateOf(twoDDoc.header().certId());

        try {
            return verifySignature(twoDDoc, signingCertificate);
        } catch (InvalidKeyException | SignatureException e) {
            throw new TwoDDocValidationException(e);
        }
    }

    private static boolean verifySignature(TwoDDoc twoDDoc, X509Certificate signingCertificate) throws InvalidKeyException, SignatureException {
        var signature = SignatureAlgorithm.of(signingCertificate).getInstance();
        signature.initVerify(signingCertificate.getPublicKey());
        signature.update(twoDDoc.getSignedBytes());
        return signature.verify(twoDDoc.signature().encodeDer());
    }

}
