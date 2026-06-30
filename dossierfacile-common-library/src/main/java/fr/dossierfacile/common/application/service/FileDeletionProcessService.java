package fr.dossierfacile.common.application.service;

import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.FileDeletionDomainService;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileDeletionProcessService {

    private final FileDeletionDomainService fileDeletionDomainService;
    private final JpaApartmentSharingRepository jpaApartmentSharingRepository;
    private final JpaTenantRepository jpaTenantRepository;

    public Optional<Document> processFileDeletion(
            Long fileId,
            Document document,
            Tenant tenant,
            ApartmentSharing apartmentSharing,
            Optional<Operator> operator
    ) {
        // 1. Suppression métier du fichier et garde-fous document
        var savedDocument = fileDeletionDomainService.deleteFile(fileId, document, tenant, operator);

        // 2. Invalidation du PDF global du dossier
        apartmentSharing.resetDossierPdfGenerated();
        jpaApartmentSharingRepository.save(apartmentSharing);

        // 3. Whe update tenant status
        tenant.updateLastUpdateDate();
        jpaTenantRepository.save(tenant);

        return savedDocument;
    }

}
