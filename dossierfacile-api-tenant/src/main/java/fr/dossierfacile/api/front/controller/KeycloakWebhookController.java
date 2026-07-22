package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.domain.service.FindOrCreateTenantDomainService;
import fr.dossierfacile.api.front.domain.service.TenantSynchronizationDomainService;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.KeycloakWebhookEvent;
import fr.dossierfacile.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook/keycloak")
@RequiredArgsConstructor
@Slf4j
public class KeycloakWebhookController {

    private final FindOrCreateTenantDomainService findOrCreateTenantDomainService;
    private final TenantSynchronizationDomainService tenantSynchronizationDomainService;

    @Value("${keycloak.webhook.token}")
    private String expectedToken;

    @PostMapping("/user-sync")
    public ResponseEntity<Void> syncUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody KeycloakWebhookEvent webhookEvent) {

        if (expectedToken == null || expectedToken.isBlank()) {
            log.error("Keycloak webhook security token is not configured in properties");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String cleanHeader = authorizationHeader != null ? authorizationHeader.trim() : "";
        String expectedBearer = "Bearer " + expectedToken.trim();

        if (!expectedBearer.equals(cleanHeader) && !expectedToken.trim().equals(cleanHeader)) {
            log.warn("Unauthorized Keycloak webhook access attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Keycloak webhook user-sync event received for type {} and userId {}", webhookEvent.getType(), webhookEvent.getUserId());

        if ("LOGIN".equals(webhookEvent.getType())) {
            KeycloakUser keycloakUser = webhookEvent.toKeycloakUser();
            Tenant tenant = findOrCreateTenantDomainService.findOrCreateTenant(keycloakUser, null);
            tenantSynchronizationDomainService.synchronizeTenant(tenant, keycloakUser);
        }
        return ResponseEntity.ok().build();
    }
}
