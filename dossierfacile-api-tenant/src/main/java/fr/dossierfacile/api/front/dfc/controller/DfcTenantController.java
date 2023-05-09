package fr.dossierfacile.api.front.dfc.controller;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.KeycloakUser;
import fr.dossierfacile.api.front.model.dfc.tenant.ConnectedTenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.service.interfaces.UserService;
import fr.dossierfacile.api.front.util.SentryUtil;
import fr.dossierfacile.common.entity.Tenant;
import io.sentry.SentryLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/dfc/tenant")
@Slf4j
public class DfcTenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantMapper tenantMapper;
    private final TenantService tenantService;
    private final UserService userService;

    @PreAuthorize("!isClient()")
    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectedTenantModel> profilePartner() {
        String partner = authenticationFacade.getKeycloakClientId();
        Tenant tenant = tenantService.findByKeycloakId(authenticationFacade.getKeycloakUserId());
        if (tenant == null) {
            log.error(SentryUtil.captureMessage("User try to connect with not found keycloakId " + authenticationFacade.getUserEmail(), SentryLevel.ERROR));
            tenant = tenantService.findByEmail(authenticationFacade.getUserEmail()).orElse(null);
        }

        if (tenant == null) {
            KeycloakUser kcUser = authenticationFacade.getKeycloakUser();
            tenant = tenantService.registerFromKeycloakUser(kcUser, partner);
        } else {
            userService.linkTenantToPartner(tenant, partner, null);
        }
        return ok(tenantMapper.toTenantModelDfc(tenant));
    }

    @RequestMapping(path = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> logout() {
        Tenant tenant = authenticationFacade.getLoggedTenant();
        userService.logout(tenant);
        return ok().build();
    }
}
