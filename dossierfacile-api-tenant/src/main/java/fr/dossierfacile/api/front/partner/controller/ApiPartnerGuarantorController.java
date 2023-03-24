package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.service.interfaces.GuarantorService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api-partner/tenant/{tenantId}/guarantor")
public class ApiPartnerGuarantorController {
    private final GuarantorService guarantorService;
    private final TenantService tenantService;

    @PreAuthorize("hasPermissionOnTenant(#tenantId)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable Long tenantId) {
        var tenant = tenantService.findById(tenantId);
        guarantorService.delete(id, tenant);
        return ResponseEntity.ok().build();
    }

}
