package fr.dossierfacile.process.file.service.monfranceconnect.client;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonFranceConnectClient {

    @Value("${monfranceconnect.api.url}")
    private String monFranceConnectBaseUrl;

    private static RestTemplate restTemplateIgnoringHttps() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);

        return new RestTemplate(requestFactory);
    }

    public Optional<DocumentVerifiedContent> fetchDocumentContent(String url) {
        DocumentVerificationRequest request = DocumentVerificationRequest.forQrCodeContent(url);
        URI uri = request.getUri(monFranceConnectBaseUrl);
        HttpEntity<String> entity = request.getHttpEntity();
        try {
            log.info("Calling MonFranceConnect at {} with body {}", uri, entity.getBody());
            ResponseEntity<String[]> response = restTemplateIgnoringHttps().exchange(uri, HttpMethod.POST, entity, String[].class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return DocumentVerifiedContent.from(response);
            }
            log.warn("MonFranceConnect responded with status {}", response.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error while calling MonFranceConnect (sentry id: {})", Sentry.captureException(e), e);
            return Optional.empty();
        }
    }
}
