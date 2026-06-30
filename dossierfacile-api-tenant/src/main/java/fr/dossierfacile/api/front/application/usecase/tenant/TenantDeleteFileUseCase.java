package fr.dossierfacile.api.front.application.usecase.tenant;

import fr.dossierfacile.api.front.domain.policy.TenantAccessPolicy;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.application.service.FileDeletionProcessService;
import fr.dossierfacile.common.application.usecase.BaseUseCase;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.TenantResolverDomainService;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

@Component
@Slf4j
public class TenantDeleteFileUseCase extends BaseUseCase<TenantDeleteFileUseCase.TenantDeleteFileCommand, Void> {

    private final JpaTenantRepository jpaTenantRepository;
    private final JpaDocumentRepository jpaDocumentRepository;
    private final JpaApartmentSharingRepository jpaApartmentSharingRepository;
    private final TenantAccessPolicy tenantAccessPolicy;
    private final FileDeletionProcessService fileDeletionProcessService;
    private final TenantResolverDomainService tenantResolverDomainService;
    private final DocumentRepository documentRepository;
    private final DocumentIAService documentIAService;

    public TenantDeleteFileUseCase(
            PlatformTransactionManager transactionManager,
            JpaTenantRepository jpaTenantRepository,
            JpaDocumentRepository jpaDocumentRepository,
            JpaApartmentSharingRepository jpaApartmentSharingRepository,
            TenantAccessPolicy tenantAccessPolicy,
            FileDeletionProcessService fileDeletionProcessService,
            TenantResolverDomainService tenantResolverDomainService,
            DocumentRepository documentRepository,
            DocumentIAService documentIAService
    ) {
        super(transactionManager);
        this.jpaTenantRepository = jpaTenantRepository;
        this.jpaDocumentRepository = jpaDocumentRepository;
        this.jpaApartmentSharingRepository = jpaApartmentSharingRepository;
        this.tenantAccessPolicy = tenantAccessPolicy;
        this.fileDeletionProcessService = fileDeletionProcessService;
        this.tenantResolverDomainService = tenantResolverDomainService;
        this.documentRepository = documentRepository;
        this.documentIAService = documentIAService;
    }

    @Override
    public Void execute(TenantDeleteFileCommand command) {
        log.info("Starting TenantDeleteFileUseCase for fileId={}, keycloakId={}", command.fileId, command.keycloakId);
        checkTransaction();

        Optional<Long> optionalDocumentId = executeInTransaction(status -> {
            var tenant = jpaTenantRepository.findByKeycloakId(command.keycloakId)
                    .orElseThrow(() -> new ModelNotFoundException(Tenant.class, command.keycloakId));
            var document = jpaDocumentRepository.findByFileId(command.fileId)
                    .orElseThrow(() -> new ModelNotFoundException(FileEntity.class, command.fileId));
            var apartmentSharing = jpaApartmentSharingRepository.findById(tenant.getApartmentSharingId())
                    .orElseThrow(() -> new ModelNotFoundException(ApartmentSharing.class, tenant.getApartmentSharingId()));

            Tenant targetTenant = tenantResolverDomainService.resolveTargetedTenant(document, tenant);

            tenantAccessPolicy.validateAccess(tenant, targetTenant, apartmentSharing);
            log.info("Access validated for tenant id={} on targetTenant id={} for fileId={}", tenant.getId(), targetTenant.getId(), command.fileId);

            // SavedDocument est empty si le document a été supprimé
            var savedDocument = fileDeletionProcessService.processFileDeletion(command.fileId, document, targetTenant, apartmentSharing, Optional.empty());
            log.info("File deletion process completed for fileId={}. Remaining document={}", command.fileId, savedDocument.isPresent());

            // Si on a un document a la sortie de la méthode (il reste des fichiers) on le retourne pour la suite du traitement
            return savedDocument.map(Document::getId);
        });

        // TODO : Il faut supprimer ce traitement quand la migration vers les entité DDD sera fini !
        // il faudra faire ce traitement dans la partie fileDeletionDomainService
        optionalDocumentId.ifPresent(documentId -> transactionTemplate.execute(status -> {
            log.info("Triggering DocumentIA analysis after commit for documentId={}", documentId);
            var document = documentRepository.getReferenceById(documentId);
            documentIAService.analyseDocument(document);
            return null;
        }));
        return null;
    }

    public record TenantDeleteFileCommand(Long fileId, String keycloakId) {
    }

}
