package fr.dossierfacile.process.file.service;

import fr.dossierfacile.process.file.model.Taxes;
import fr.dossierfacile.process.file.service.interfaces.ApiParticulier;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiParticulierImpl implements ApiParticulier {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private final RestTemplate restTemplate;
    @Value("${particulier.api.url}")
    private String apiURL;
    @Value("${particulier.api.gouv.fr.token}")
    private String apiToken;

    @Override
    public ResponseEntity<Taxes> particulierApi(String fiscalNumber, String taxReference) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            String url = apiURL + "/v2/avis-imposition?numeroFiscal=" + fiscalNumber + "&referenceAvis=" + taxReference;
            log.info("\nurl: {}", url);
            log.info("\ntoken: {}", apiToken);
            long time = System.currentTimeMillis();
            ResponseEntity<Taxes> response = restTemplate.exchange(url, HttpMethod.GET, entity, Taxes.class);
            log.info("Time call api particuler " + (System.currentTimeMillis() - time) + " ms");
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
            return ResponseEntity.notFound().build();
        }
    }
}
