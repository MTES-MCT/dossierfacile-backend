package fr.dossierfacile.scheduler.tasks.analytics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Slf4j
@Service
public class DbtTriggerService {

    private final AnalyticsProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public DbtTriggerService(AnalyticsProperties properties) {
        this(properties, new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .build());
    }

    public DbtTriggerService(AnalyticsProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public void trigger() {
        String webhookUrl = properties.getDbtWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.info("Pas de trigger DBT, skip.");
            return;
        }

        log.info("Déclenchement du webhook dbt vers {}", webhookUrl);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String token = properties.getDbtWebhookToken();
            if (token != null && !token.isBlank()) {
                String authHeader = token.startsWith("Token ") ? token : "Token " + token;
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            }

            String body = "{\"cause\": \"Triggered via API\"}";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("Webhook dbt exécuté avec succès. Statut HTTP : {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Erreur lors de l'appel au webhook dbt: {}", e.getMessage(), e);
        }
    }
}
