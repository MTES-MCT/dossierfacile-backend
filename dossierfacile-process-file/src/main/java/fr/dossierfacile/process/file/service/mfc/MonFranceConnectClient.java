package fr.dossierfacile.process.file.service.mfc;

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
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonFranceConnectClient {
    private static final String EXCEPTION = "Sentry ID Exception: ";

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

    public ResponseEntity<List> fetchDocumentContent(String url) {
        DocumentVerificationRequest request = DocumentVerificationRequest.forQrCodeContent(url);
        URI uri = request.getUri(monFranceConnectBaseUrl);
        HttpEntity<String> entity = request.getHttpEntity();
        try {
            long time = System.currentTimeMillis();
            ResponseEntity<List> response = restTemplateIgnoringHttps().exchange(uri, HttpMethod.POST, entity, List.class);
            long milliseconds = System.currentTimeMillis() - time;
            log.info("Time call api MonFranceConnect " + milliseconds + " milliseconds");
            return response;
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
            return ResponseEntity.notFound().build();
        }
    }
}
