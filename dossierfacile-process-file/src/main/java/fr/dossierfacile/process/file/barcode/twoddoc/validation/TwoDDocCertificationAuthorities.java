package fr.dossierfacile.process.file.barcode.twoddoc.validation;

import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    public X509Certificate getCertificateUsedFor(TwoDDocHeader twoDDocHeader) {
        String certId = twoDDocHeader.certId();
        if (!certificates.containsKey(certId)) {
            X509Certificate certificate = fetchCertificate(twoDDocHeader);
            certificates.put(certId, certificate);
        }
        return certificates.get(certId);
    }

    private X509Certificate fetchCertificate(TwoDDocHeader twoDDocHeader) {
        try {
            URI uri = getCertificateUri(twoDDocHeader);
            ResponseEntity<byte[]> entity = restTemplate.getForEntity(uri, byte[].class);
            X509Certificate certificate = parseX509Certificate(entity.getBody());
            certificate.checkValidity();
            return certificate;
        } catch (CertificateException e) {
            throw new TwoDDocValidationException("Invalid signing certificate for " + twoDDocHeader, e);
        }
    }

    private URI getCertificateUri(TwoDDocHeader twoDDocHeader) {
        // TODO use ANTS TLS instead of hard coded urls
        String baseUri = switch (twoDDocHeader.issuer()) {
            case "FR03" -> "http://certificates.certigna.fr/search.php?iHash=xvNLC1KMs03t%2FgxzdBYParPnf%2BM";
            case "FR04" -> "http://pki-g2.ariadnext.fr/pki-2ddoc.der";
            default -> throw new TwoDDocValidationException("Unsupported certification authority");
        };
        return UriComponentsBuilder.fromUriString(baseUri)
                .queryParam("name", twoDDocHeader.certId())
                .build().toUri();
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
