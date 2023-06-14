package fr.dossierfacile.process.file.barcode.twoddoc.validation;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.X509Certificate;

public enum SignatureAlgorithm {

    SHA256withECDSA,
    SHA256withRSA
    ;

    public static SignatureAlgorithm of(X509Certificate certificate) {
        String algorithm = certificate.getPublicKey().getAlgorithm();
        return "EC".equals(algorithm) ? SHA256withECDSA : SHA256withRSA;
    }

    public Signature getInstance() {
        try {
            return Signature.getInstance(name());
        } catch (NoSuchAlgorithmException e) {
            // Should never happen since all enum members are valid names
            throw new RuntimeException("Invalid signature algorithm name");
        }
    }
}
