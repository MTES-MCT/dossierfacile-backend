package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantResolverDomainService {

    private final JpaTenantRepository jpaTenantRepository;

    public Tenant resolveTargetedTenant(Document document) {
        return resolveTargetedTenant(document, null);
    }

    public Tenant resolveTargetedTenant(Document document, Tenant currentTenant) {
        if (currentTenant != null && document.getTenantId() != null && document.getTenantId().equals(currentTenant.getId())) {
            return currentTenant;
        }
        if (document.getTenantId() == null) {
            return jpaTenantRepository.findByGuarantorId(document.getGuarantorId())
                    .orElseThrow(() -> new ModelNotFoundException(Tenant.class, document.getGuarantorId()));
        }
        return jpaTenantRepository.findById(document.getTenantId())
                .orElseThrow(() -> new ModelNotFoundException(Tenant.class, document.getTenantId()));
    }
}
