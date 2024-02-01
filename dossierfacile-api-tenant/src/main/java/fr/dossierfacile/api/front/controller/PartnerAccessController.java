package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.config.featureflipping.PartnerAccessRevocationToggle;
import fr.dossierfacile.api.front.model.tenant.PartnerAccessModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.PartnerAccessService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/tenant/partners")
@Slf4j
public class PartnerAccessController {

    private final AuthenticationFacade authenticationFacade;
    private final PartnerAccessService partnerAccessService;
    private final PartnerAccessRevocationToggle toggle;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PartnerAccessModel>> getPartnerAccesses() {
        if (toggle.isNotActive()) {
            return ok(List.of());
        }
        Tenant tenant = authenticationFacade.getLoggedTenant();
        return ok(partnerAccessService.getExternalPartners(tenant));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokePartnerAccess(@PathVariable("id") Long userApiId) {
        log.info("Requesting access revocation for partner {}", userApiId);
        if (toggle.isNotActive()) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        Tenant tenant = authenticationFacade.getLoggedTenant();
        partnerAccessService.deleteAccess(tenant, userApiId);
        return ok().build();
    }

}
