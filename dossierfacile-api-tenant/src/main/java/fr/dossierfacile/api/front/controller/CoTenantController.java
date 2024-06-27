package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@RequestMapping("/api/tenant/coTenant")
public class CoTenantController {

    private final AuthenticationFacade authenticationFacade;
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private final ClientAuthenticationFacade clientAuthenticationFacade;

    @PreAuthorize("hasPermissionOnTenant(#coTenantId)")
    @GetMapping(value = "/{id}/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantModel> tenantProfile(@PathVariable("id") Long coTenantId) {
        Tenant coTenant = tenantService.findById(coTenantId);
        tenantService.updateLastLoginDateAndResetWarnings(coTenant);
        return ok(tenantMapper.toTenantModel(coTenant, null));
    }
}
