package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.gouv.bo.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * REST controller for E2E testing endpoints.
 * Only active in dev, preprod, and test environments.
 * Explicitly blocked in production.
 */
@RestController
@RequestMapping("/api/testing")
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "preprod", "test", "!prod"})
public class BOE2ETestController {

    private final TenantService tenantService;
    private final Environment environment;

    @Value("${testing.api.allowed-tenant-email}")
    private String allowedTenantEmail;

    @PostMapping("/tenant/{tenantEmail}/validate")
    public ResponseEntity<Void> validateTenant(@PathVariable String tenantEmail) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            log.error("E2E testing endpoint called in PRODUCTION environment");
            return ResponseEntity.status(403).build();
        }

        if (!tenantEmail.equals(allowedTenantEmail)){
            log.warn("Attempted to validate non-test tenant: {}", tenantEmail);
            return ResponseEntity.badRequest().build();
        }

        try {
            Tenant tenant = tenantService.findTenantByEmail(tenantEmail);
            tenantService.validateTenantForTesting(tenant.getId());
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            log.error("Tenant not found: {}", tenantEmail);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error validating tenant: {}", tenantEmail, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
