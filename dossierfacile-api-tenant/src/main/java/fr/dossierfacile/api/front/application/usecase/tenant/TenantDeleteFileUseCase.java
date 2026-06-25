package fr.dossierfacile.api.front.application.usecase.tenant;

import fr.dossierfacile.api.front.application.exception.ModelNotFoundException;
import fr.dossierfacile.api.front.application.usecase.BaseUseCase;
import fr.dossierfacile.api.front.domain.policy.TenantAccessPolicy;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.FileDeletionDomainService;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

@Component
public class TenantDeleteFileUseCase extends BaseUseCase<TenantDeleteFileUseCase.TenantDeleteFileCommand, Void> {

    private final JpaTenantRepository jpaTenantRepository;
    private final JpaDocumentRepository jpaDocumentRepository;
    private final JpaApartmentSharingRepository jpaApartmentSharingRepository;
    private final TenantAccessPolicy tenantAccessPolicy;
    private final FileDeletionDomainService fileDeletionDomainService;
    private final DocumentRepository documentRepository;
    private final DocumentIAService documentIAService;

    public TenantDeleteFileUseCase(
            PlatformTransactionManager transactionManager,
            JpaTenantRepository jpaTenantRepository,
            JpaDocumentRepository jpaDocumentRepository,
            JpaApartmentSharingRepository jpaApartmentSharingRepository,
            TenantAccessPolicy tenantAccessPolicy,
            FileDeletionDomainService fileDeletionDomainService,
            DocumentRepository documentRepository,
            DocumentIAService documentIAService
    ) {
        super(transactionManager);
        this.jpaTenantRepository = jpaTenantRepository;
        this.jpaDocumentRepository = jpaDocumentRepository;
        this.jpaApartmentSharingRepository = jpaApartmentSharingRepository;
        this.tenantAccessPolicy = tenantAccessPolicy;
        this.fileDeletionDomainService = fileDeletionDomainService;
        this.documentRepository = documentRepository;
        this.documentIAService = documentIAService;
    }

    @Override
    public Void execute(TenantDeleteFileCommand command) {
        checkTransaction();

        Optional<Long> optionalDocumentId = transactionTemplate.execute(status -> {
            var tenant = jpaTenantRepository.findByKeycloakId(command.keycloakId)
                    .orElseThrow(() -> new ModelNotFoundException(Tenant.class, command.keycloakId));
            var document = jpaDocumentRepository.findByFileId(command.fileId)
                    .orElseThrow(() -> new ModelNotFoundException(FileEntity.class, command.fileId));
            var applicationSharing = jpaApartmentSharingRepository.findById(tenant.getApartmentSharingId())
                    .orElseThrow(() -> new ModelNotFoundException(ApartmentSharing.class, tenant.getApartmentSharingId()));

            Tenant targetTenant = loadTargetedTenant(document, tenant);

            tenantAccessPolicy.validateAccess(tenant, targetTenant, applicationSharing);

            // SavedDocument est empty si le document a été supprimé
            var savedDocument = fileDeletionDomainService.deleteFile(command.fileId, document, targetTenant);

            tenant.updateLastUpdateDate();
            jpaTenantRepository.save(tenant);

            // Si on a un document a la sortie de la méthode (il reste des fichiers) on le retourne pour la suite du traitement
            return savedDocument.map(Document::getId);
        });

        // TODO : Il faut supprimer ce traitement quand la migration vers les entité DDD sera fini !
        // il faudra faire ce traitement dans la partie fileDeletionDomainService
        if (optionalDocumentId != null && optionalDocumentId.isPresent()) {
            transactionTemplate.execute(status -> {
                var document = documentRepository.getReferenceById(optionalDocumentId.get());
                documentIAService.analyseDocument(document);
                return null;
            });
        }
        return null;
    }

    private Tenant loadTargetedTenant(Document document, Tenant tenant) {
        if (document.getTenantId().equals(tenant.getId())) {
            return tenant;
        }
        if (document.getTenantId() == null) {
            return jpaTenantRepository.findByGuarantorId(document.getGuarantorId())
                    .orElseThrow(() -> new ModelNotFoundException(Tenant.class, document.getGuarantorId()));
        }
        return jpaTenantRepository.findById(document.getTenantId())
                .orElseThrow(() -> new ModelNotFoundException(Tenant.class, document.getTenantId()));
    }

    public record TenantDeleteFileCommand(Long fileId, String keycloakId) {
    }

}
