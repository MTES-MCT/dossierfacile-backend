package fr.dossierfacile.api.front.dfc.service;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DfcDocumentServiceImpl implements DfcDocumentService {

    private final TenantService tenantService;
    private final TenantPermissionsService tenantPermissionsService;
    private final DocumentRepository documentRepository;

    @Override
    public Document getAuthorizedDocument(String token, Long tenantId, String keycloakClientId) {
        Tenant tenant = tenantService.findById(tenantId);
        if (tenant == null || tenant.getApartmentSharing() == null) {
            throw new TenantNotFoundException(tenantId);
        }

        // The tenant must have agreed to share with this specific partner
        if (!tenantPermissionsService.clientCanAccess(keycloakClientId, tenantId)) {
            throw new AccessDeniedException("Partner not authorized to access this tenant");
        }

        // Same scope as the public share-link endpoint (/api/application/links/{token}/documents/{name}):
        // any document belonging to the tenant's apartment sharing - co-tenants and guarantors included.
        Document document = documentRepository.findByNameForApartmentSharing(token, tenant.getApartmentSharing().getId())
                .orElseThrow(() -> new DocumentNotFoundException(token));
        if (document.getWatermarkFile() == null) {
            throw new DocumentNotFoundException(token);
        }
        return document;
    }
}
