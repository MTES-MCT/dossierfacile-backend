package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.exceptions.NotFoundException;
import fr.gouv.bo.service.KeycloakService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * REST controller for E2E testing endpoints.
 * Only active in dev, preprod, and test environments.
 * Explicitly blocked in production.
 * <p>
 * Endpoints only operate on accounts whose email matches the configured
 * allowlist pattern, and operations requiring an operator are attributed to a
 * dedicated e2e operator account.
 * Naming: historical endpoints live under /tenant/{email}/..., newer
 * account-management ones under /user/{email}/....
 */
@RestController
@RequestMapping("/api/testing")
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "preprod", "test", "!prod"})
public class BOE2ETestController {

    private final TenantService tenantService;
    private final UserService userService;
    private final KeycloakService keycloakService;
    private final Environment environment;

    @Value("${testing.api.allowed-email-pattern}")
    private String allowedEmailPattern;

    @Value("${testing.api.operator-email}")
    private String operatorEmail;

    private Pattern allowedEmails;

    @PostConstruct
    void compileAllowedEmailPattern() {
        allowedEmails = Pattern.compile(allowedEmailPattern, Pattern.CASE_INSENSITIVE);
    }

    @PostMapping("/tenant/{tenantEmail}/validate")
    public ResponseEntity<Void> validateTenant(@PathVariable String tenantEmail) {
        ResponseEntity<Void> rejection = rejectIfForbidden(tenantEmail, "validate");
        if (rejection != null) {
            return rejection;
        }

        try {
            Tenant tenant = tenantService.findTenantByEmail(tenantEmail);
            tenantService.validateTenantFile(tenant.getId(), e2eOperator());
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            log.error("Tenant not found: {}", tenantEmail);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error validating tenant: {}", tenantEmail, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/tenant/{tenantEmail}/decline")
    public ResponseEntity<Void> declineTenant(@PathVariable String tenantEmail,
                                              @RequestBody DeclineRequest body) {
        ResponseEntity<Void> rejection = rejectIfForbidden(tenantEmail, "decline");
        if (rejection != null) {
            return rejection;
        }

        try {
            Tenant tenant = tenantService.findTenantByEmail(tenantEmail);
            List<DocumentCategory> categories = body.documentCategories() == null
                    ? Collections.emptyList()
                    : body.documentCategories().stream().map(DocumentCategory::valueOf).toList();
            tenantService.declineTenantForTesting(tenant.getId(), e2eOperator(), body.messageBody(), categories);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            log.error("Tenant not found: {}", tenantEmail);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error declining tenant: {}", tenantEmail, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Marks the Keycloak account as verified so e2e tests can sign up through
     * the UI without going through the verification email.
     */
    @PostMapping("/user/{email}/verify-email")
    public ResponseEntity<Void> verifyEmail(@PathVariable String email) {
        ResponseEntity<Void> rejection = rejectIfForbidden(email, "verify email of");
        if (rejection != null) {
            return rejection;
        }

        try {
            return keycloakService.markEmailAsVerified(email)
                    ? ResponseEntity.ok().build()
                    : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error verifying email: {}", email, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Idempotent reset of a test account: deletes the tenant (and its whole
     * apartment sharing) if present, and the Keycloak account either way.
     */
    @DeleteMapping("/user/{email}")
    public ResponseEntity<Void> deleteUser(@PathVariable String email) {
        ResponseEntity<Void> rejection = rejectIfForbidden(email, "delete");
        if (rejection != null) {
            return rejection;
        }

        try {
            boolean deleted = tenantService.findTenantByEmailOptional(email)
                    .map(tenant -> {
                        userService.deleteApartmentSharing(tenant, e2eOperator());
                        return true;
                    })
                    .orElse(false);
            // A Keycloak account may exist without a tenant in DB: the tenant is
            // only created on the first authenticated API call after signup
            deleted |= keycloakService.deleteKeycloakUserByEmail(email);
            return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting test user: {}", email, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Void> rejectIfForbidden(String email, String action) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            log.error("E2E testing endpoint called in PRODUCTION environment");
            return ResponseEntity.status(403).build();
        }
        if (!allowedEmails.matcher(email).matches()) {
            log.warn("Attempted to {} non-test user: {}", action, email);
            return ResponseEntity.badRequest().build();
        }
        return null;
    }

    private BOUser e2eOperator() {
        return userService.findOrCreateOperatorByEmail(operatorEmail);
    }

    public record DeclineRequest(String messageBody, List<String> documentCategories) {}
}
