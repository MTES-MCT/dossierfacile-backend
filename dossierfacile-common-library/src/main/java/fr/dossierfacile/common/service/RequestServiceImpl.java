package fr.dossierfacile.common.service;

import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.service.interfaces.RequestService;
import fr.dossierfacile.common.validator.ValidUrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private static final String X_API_KEY = "x-api-key";
    private static final String CALL_BACK_RESPONSE = "CallBack ResponseStatus: {}";
    private final RestTemplate restTemplate;

    @Async
    public void send(ApplicationModel applicationModel, String urlCallback, String partnerApiKeyCallback) {
        if (!ValidUrlValidator.isValidUrl(urlCallback)) {
            log.warn("Blocked callback request to invalid or forbidden URL: {}", urlCallback);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        if (partnerApiKeyCallback != null && !partnerApiKeyCallback.isEmpty()) {
            headers.set(X_API_KEY, partnerApiKeyCallback);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        sendRequest(new HttpEntity<>(applicationModel, headers), urlCallback);
    }

    private <T> void sendRequest(HttpEntity<T> request, String urlCallback) {
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.exchange(urlCallback, HttpMethod.POST, request, String.class);
            log.info(CALL_BACK_RESPONSE, response.getStatusCode());
        } catch (RestClientException e) {
            log.error("Error occurs during the call to :" + urlCallback, e);
        }
        if (response != null
                && HttpStatus.OK != response.getStatusCode()
                && HttpStatus.ACCEPTED != response.getStatusCode()
                && HttpStatus.NO_CONTENT != response.getStatusCode()) {
            log.error("Failure on partner callback url:" + urlCallback + "- Status:" + response.getStatusCode());
        }
    }
}
