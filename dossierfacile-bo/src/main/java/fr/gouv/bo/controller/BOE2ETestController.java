package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.gouv.bo.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for testing endpoints.
 * Only active in dev and preprod environments.
 * This controller provides endpoints for E2E testing purposes.
 */
@RestController
@RequestMapping("/api/testing")
@RequiredArgsConstructor
@Slf4j
@Profile({"dev"})
public class BOE2ETestController {

    private final TenantService tenantService;

    @Value("${testing.api.allowed-tenant-email}")
    private String allowedTenantEmail;

    @PostMapping("/tenant/{tenantEmail}/validate")
    public ResponseEntity<Void> validateTenant(@PathVariable String tenantEmail) {
        log.info("Testing endpoint: validating tenant with tenantEmail: {}", tenantEmail);

        if (!tenantEmail.equals(allowedTenantEmail)){
            log.warn("Attempted to validate non-test tenant: {}", tenantEmail);
            return ResponseEntity.badRequest().build();
        }

        try {
            Tenant tenant = tenantService.findTenantByEmail(tenantEmail);
            tenantService.validateTenantForTesting(tenant.getId());
            log.info("Successfully validated test tenant: {}", tenantEmail);
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

