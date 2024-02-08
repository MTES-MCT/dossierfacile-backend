package fr.dossierfacile.process.file.barcode.twoddoc.validation;

import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TwoDDocCertificationAuthorities {

    private final AntsTrustServiceList trustServiceList;
    private final Map<String, X509Certificate> certificatesById = new HashMap<>();
    private final RestTemplate restTemplate;

    public X509Certificate getCertificateUsedFor(TwoDDocHeader twoDDocHeader) {
        String certId = twoDDocHeader.certId();
        if (!certificatesById.containsKey(certId)) {
            X509Certificate certificate = fetchCertificate(twoDDocHeader);
            certificatesById.put(certId, certificate);
        }
        return certificatesById.get(certId);
    }

    private X509Certificate fetchCertificate(TwoDDocHeader twoDDocHeader) {
        try {
            URI uri = trustServiceList.getCertificateUri(twoDDocHeader);
            ResponseEntity<byte[]> entity = restTemplate.getForEntity(uri, byte[].class);
            X509Certificate certificate = parseX509Certificate(entity.getBody());
            checkValidity(certificate);
            return certificate;
        } catch (CertificateException e) {
            throw new TwoDDocValidationException("Invalid signing certificate for " + twoDDocHeader, e);
        }
    }

    private X509Certificate parseX509Certificate(byte[] bytes) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return (X509Certificate) certFactory.generateCertificate(inputStream);
        } catch (IOException e) {
            throw new TwoDDocValidationException(e);
        }
    }

    private void checkValidity(X509Certificate certificate) {
        try {
            certificate.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            log.warn("Certificate is currently not valid: {}", e.getMessage());
        }
    }

}
