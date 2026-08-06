package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@AllArgsConstructor
public class FileDeletionDomainService {

    private final AddLogDomainService addLogDomainService;
    private final JpaDocumentRepository jpaDocumentRepository;
    private final MessagePublisher messagePublisher;
    private final JpaApartmentSharingRepository jpaApartmentSharingRepository;
    private final JpaTenantRepository jpaTenantRepository;
    private final CheckDocumentForReprocessingDomainService checkDocumentForReprocessingDomainService;

    public Optional<Document> deleteFile(
            Long fileId,
            Document document,
            Tenant targetTenant,
            ApartmentSharing apartmentSharing,
            Optional<Operator> operator
    ) {
        FileEntity file = document.getFileById(fileId);

        addLogDomainService.addFileDeletedLog(file, targetTenant, operator);

        document.deleteFile(fileId);
        // Si le document n'est pas vide on re-analyse le document
        if (document.hasFiles()) {
            // TODO : replace with domain service PublishQueueMessageDomainService
            messagePublisher.sendDocumentForPdfGeneration(document.getId());
            jpaDocumentRepository.save(document);
            updateTenantAndApartmentSharing(targetTenant, apartmentSharing);
            return Optional.of(document);
        }

        handleEmptyDocument(document, targetTenant, operator);
        updateTenantAndApartmentSharing(targetTenant, apartmentSharing);
        return Optional.empty();
    }

    private void handleEmptyDocument(Document document, Tenant targetTenant, Optional<Operator> operator) {
        // Si le document est vide, on le supprime.
        addLogDomainService.addDocumentDeletedLog(document, targetTenant, operator);

        checkDocumentForReprocessingDomainService.checkDocumentsForReprocessing(document);

        jpaDocumentRepository.delete(document);
    }

    private void updateTenantAndApartmentSharing(Tenant targetTenant, ApartmentSharing apartmentSharing) {
        apartmentSharing.resetDossierPdfGenerated();
        jpaApartmentSharingRepository.save(apartmentSharing);

        targetTenant.updateLastUpdateDate();
        jpaTenantRepository.save(targetTenant);
    }
}
