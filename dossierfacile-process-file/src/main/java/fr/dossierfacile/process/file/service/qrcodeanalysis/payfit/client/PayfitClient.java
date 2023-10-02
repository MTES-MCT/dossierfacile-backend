package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
public class PayfitClient {

    private final RestTemplate restTemplate;
    private final String payfitAuthenticationUrl;

    public PayfitClient(RestTemplate restTemplate, @Value("${payfit.api.url}") String payfitAuthenticationUrl) {
        this.restTemplate = restTemplate;
        this.payfitAuthenticationUrl = payfitAuthenticationUrl;
    }

    public Optional<PayfitResponse> getVerifiedContent(PayfitAuthenticationRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>(request, headers);

        try {
            log.info("Calling PayFit at {} with body {}", payfitAuthenticationUrl, request);
            ResponseEntity<PayfitResponse> response = restTemplate.exchange(payfitAuthenticationUrl,
                    HttpMethod.POST, entity, PayfitResponse.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(response.getBody());
            }
            log.warn("PayFit responded with {}", response.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error while calling PayFit (sentry id: {})", Sentry.captureException(e), e);
            return Optional.empty();
        }
    }

}
