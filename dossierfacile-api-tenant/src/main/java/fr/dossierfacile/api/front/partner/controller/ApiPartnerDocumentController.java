package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api-partner/tenant/{tenantId}/document")
public class ApiPartnerDocumentController {
    private final DocumentService documentService;
    private final TenantService tenantService;

    @PreAuthorize("hasPermissionOnTenant(#tenantId)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable Long tenantId) {
        var tenant = tenantService.findById(tenantId);
        documentService.delete(id, tenant);
        return ResponseEntity.ok().build();
    }
}
