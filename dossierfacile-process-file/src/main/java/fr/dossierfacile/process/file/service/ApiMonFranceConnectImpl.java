package fr.dossierfacile.process.file.service;

import fr.dossierfacile.process.file.service.interfaces.ApiMonFranceConnect;
import io.sentry.Sentry;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiMonFranceConnectImpl implements ApiMonFranceConnect {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private static final String URI_VARIABLE = "/sums";
    private static final String QUERY_PARAM = "idJustificatif=";

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

    @Override
    public ResponseEntity<List> monFranceConnect(String url) {
        URI uri = URI.create(url).resolve(url);
        List<NameValuePair> params = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
        String query = "";
        String body = "";
        for (NameValuePair param : params) {
            if (param.getName().equals("id")) {
                query = QUERY_PARAM + param.getValue();
            } else if (param.getName().equals("data")) {
                body = param.getValue();
            }
            log.info(param.getName() + " : " + param.getValue());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        try {
            String nextUrlToCall = new URI(uri.getScheme(), null, uri.getHost(), -1, URI_VARIABLE + uri.getPath(), query, null).toString();
            log.info("\n\nurl: {}\n", nextUrlToCall);

            long time = System.currentTimeMillis();
            ResponseEntity<List> response = restTemplateIgnoringHttps().exchange(nextUrlToCall, HttpMethod.POST, entity, List.class);
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
