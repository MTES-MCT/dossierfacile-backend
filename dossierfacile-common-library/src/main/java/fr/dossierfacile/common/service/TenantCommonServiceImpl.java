package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class TenantCommonServiceImpl implements TenantCommonService {

    private final ApartmentSharingRepository apartmentSharingRepository;
    private final DocumentCommonRepository documentRepository;
    private final TenantCommonRepository tenantCommonRepository;
    private ApartmentSharingCommonService apartmentSharingCommonService;

    @Override
    public void deleteTenantData(Tenant tenant) {

        Optional<ApartmentSharing> apartmentSharing = apartmentSharingRepository.findByTenant(tenant.getId());
        if (apartmentSharing.isEmpty()) {
            return;
        }

        if (apartmentSharing.get().getPdfDossierFile() != null) {
            apartmentSharingCommonService.resetDossierPdfGenerated(apartmentSharing.get());
        }

        documentRepository.deleteAll(tenant.getDocuments());

        Optional.ofNullable(tenant.getGuarantors())
                .orElse(new ArrayList<>())
                .forEach(guarantor -> documentRepository.deleteAll(guarantor.getDocuments()));
    }

    @Override
    public Tenant findByKeycloakId(String keycloakId) {
        return tenantCommonRepository.findByKeycloakId(keycloakId);
    }

    @Override
    public Long getTenantRank(Long id) {
        return tenantCommonRepository.getTenantRank(id);
    }
}
