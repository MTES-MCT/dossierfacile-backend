package fr.dossierfacile.dfcdemo.web;

import fr.dossierfacile.dfcdemo.config.DfcDemoProperties;
import fr.dossierfacile.dfcdemo.webhook.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Receives the webhooks DossierFacile sends on each dossier event. DossierFacile expects a 200, 202 or 204 answer
 * and does not retry.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    static final String API_KEY_HEADER = "x-api-key";

    private final DfcDemoProperties properties;
    private final WebhookLogRepository repository;

    @PostMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receive(
            @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
            @RequestBody(required = false) String body) {
        if (!isAuthorized(apiKey)) {
            log.warn("Webhook rejected: missing or invalid {} header", API_KEY_HEADER);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès refusé : vous devez fournir une x-api-key valide.");
        }
        if (body == null || body.isBlank()) {
            return ResponseEntity.badRequest().body("Erreur : aucun contenu reçu dans le corps de la requête.");
        }
        repository.insert(body);
        return ResponseEntity.ok("Nouvel enregistrement créé avec succès.");
    }

    private boolean isAuthorized(String apiKey) {
        String expected = properties.webhookApiKey();
        if (apiKey == null || apiKey.isBlank() || expected == null || expected.isBlank()) {
            return false;
        }
        // Constant-time comparison so the key cannot be guessed byte by byte from response timings
        return MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
