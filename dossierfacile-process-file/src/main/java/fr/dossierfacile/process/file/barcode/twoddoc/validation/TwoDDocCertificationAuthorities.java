package fr.dossierfacile.process.file.barcode.twoddoc.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TwoDDocCertificationAuthorities {

    private final Map<String, X509Certificate> certificates = new HashMap<>();
    private final RestTemplate restTemplate;

    public X509Certificate getCertificateOf(String certificationAuthorityId) {
        if (!certificates.containsKey(certificationAuthorityId)) {
            X509Certificate certificate = fetchCertificateOf(certificationAuthorityId);
            certificates.put(certificationAuthorityId, certificate);
        }
        return certificates.get(certificationAuthorityId);
    }

    private X509Certificate fetchCertificateOf(String certificationAuthorityId) {
        try {
            ResponseEntity<byte[]> entity = restTemplate.getForEntity(uriOf(certificationAuthorityId), byte[].class);
            X509Certificate certificate = parseX509Certificate(entity.getBody());
            certificate.checkValidity();
            return certificate;
        } catch (CertificateException e) {
            throw new TwoDDocValidationException("Invalid signing certificate for authority " + certificationAuthorityId, e);
        }
    }

    private URI uriOf(String certificationAuthorityId) {
        // TODO use ANTS TLS instead of hard coded urls
        return switch (certificationAuthorityId) {
            case "FPE3" -> URI.create("http://pki-g2.ariadnext.fr/pki-2ddoc.der?name=" + certificationAuthorityId);
            default -> throw new TwoDDocValidationException("Unsupported certification authority");
        };
    }

    private X509Certificate parseX509Certificate(byte[] bytes) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try(InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return (X509Certificate) certFactory.generateCertificate(inputStream);
        } catch (IOException e) {
            throw new TwoDDocValidationException(e);
        }
    }
    
}
