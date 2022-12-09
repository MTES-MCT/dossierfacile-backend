package fr.dossierfacile.common.service;

import fr.dossierfacile.common.model.LightAPIInfoModel;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.service.interfaces.RequestService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private static final String X_API_KEY = "x-api-key";
    private static final String CALL_BACK_RESPONSE = "CallBack ResponseStatus: {}";
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private final RestTemplate restTemplate;

    @Async
    public void send(LightAPIInfoModel lightAPIInfo, String urlCallback, String partnerApiKeyCallback) {
        HttpHeaders headers = new HttpHeaders();
        if (partnerApiKeyCallback != null && !partnerApiKeyCallback.isEmpty()) {
            headers.set(X_API_KEY, partnerApiKeyCallback);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        sendRequest(new HttpEntity<>(lightAPIInfo, headers), urlCallback);
    }

    @Async
    public void send(ApplicationModel applicationModel, String urlCallback, String partnerApiKeyCallback) {
        HttpHeaders headers = new HttpHeaders();
        if (partnerApiKeyCallback != null && !partnerApiKeyCallback.isEmpty()) {
            headers.set(X_API_KEY, partnerApiKeyCallback);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        sendRequest(new HttpEntity<>(applicationModel, headers), urlCallback);
    }

    private <T> void sendRequest(HttpEntity<T> request, String urlCallback) {
        ResponseEntity<HashMap> response;
        try {
            response = restTemplate.exchange(urlCallback, HttpMethod.POST, request, HashMap.class);
            log.info(CALL_BACK_RESPONSE, response.getStatusCode());
        } catch (RestClientException e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error("Error occurs during the call to :" + urlCallback, e);
            if (e instanceof ResourceAccessException) {
                try {
                    log.warn("Trying to send the request again without SSL Verification, to the urlCallBack: " + urlCallback);
                    addExceptionOfSecurity();
                    response = restTemplate.exchange(urlCallback, HttpMethod.POST, request, HashMap.class);
                    log.info(CALL_BACK_RESPONSE, response.getStatusCode());
                } catch (GeneralSecurityException | ResourceAccessException e1) {
                    log.error(EXCEPTION + Sentry.captureException(e1));
                    log.error(e1.getClass().getName());
                }
            }
        }
    }

    private void addExceptionOfSecurity() throws GeneralSecurityException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        restTemplate.setRequestFactory(requestFactory);
    }
}
