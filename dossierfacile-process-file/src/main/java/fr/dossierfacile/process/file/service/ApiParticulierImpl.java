package fr.dossierfacile.process.file.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.process.file.model.Taxes;
import fr.dossierfacile.process.file.service.interfaces.ApiParticulier;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiParticulierImpl implements ApiParticulier {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private final RestTemplate restTemplate;
    @Value("${api.impots.url}")
    private String apiURL;
    @Value("${api.impots.token}")
    private String token;

    @Value("${api.impots.idteleservice}")
    private String idTeleservice;

    @Override
    public ResponseEntity<Taxes> particulierApi(String fiscalNumber) {
        var bearerToken = "";
        try {
            bearerToken = getBearerToken();
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
            return ResponseEntity.notFound().build();
        }

        UUID uuid = UUID.randomUUID();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + bearerToken);
        headers.set("X-Correlation-ID", uuid.toString());
        headers.set("ID_Teleservice", idTeleservice);
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        HttpEntity<?> entity = new HttpEntity<>(null, headers);
        int year = LocalDate.now().getYear();
        Month currentMonth = LocalDate.now().getMonth();
        if (currentMonth.getValue() < 6) {
            year--;
        }
        try {
            Taxes taxes = getTaxes(year, fiscalNumber, entity);
            if (taxes == null) {
                taxes = getTaxes(year - 1, fiscalNumber, entity);
            }
            return ResponseEntity.ok(taxes);
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
            return ResponseEntity.notFound().build();
        }
    }

    private Taxes getTaxes(int year, String fiscalNumber, HttpEntity<?> entity) {
        try {
            ResponseEntity<HashMap> response =
                    restTemplate.exchange(apiURL + "/impotparticulier/1.0/spi/" + fiscalNumber + "/situations/ir/factures/annrev/" + year, HttpMethod.GET, entity, HashMap.class);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(response.getBody(), Taxes.class);
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
            return null;
        }
    }


    private String getBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", "RessourceIRFacture");
        HttpEntity<?> entity = new HttpEntity<Object>(body, headers);
        ResponseEntity<HashMap> response = restTemplate.exchange(apiURL + "/token", HttpMethod.POST, entity, HashMap.class);
        Objects.requireNonNull(response.getBody());
        return Objects.requireNonNull(response.getBody().get("access_token")).toString();
    }
}
