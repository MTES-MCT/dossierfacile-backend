package fr.dossierfacile.api.front.application.usecase.tenant;

import fr.dossierfacile.api.front.domain.policy.TenantAccessPolicy;
import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.domain.service.FileDeletionDomainService;
import fr.dossierfacile.common.application.usecase.BaseUseCase;
import fr.dossierfacile.common.domain.event.DocumentModifiedEvent;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.TenantResolverDomainService;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final FileDeletionDomainService fileDeletionDomainService;
    private final TenantResolverDomainService tenantResolverDomainService;
    private final UpdateTenantStatusDomainService updateTenantStatusDomainService;
    private final ApplicationEventPublisher eventPublisher;

    public TenantDeleteFileUseCase(
            PlatformTransactionManager transactionManager,
            JpaTenantRepository jpaTenantRepository,
            JpaDocumentRepository jpaDocumentRepository,
            JpaApartmentSharingRepository jpaApartmentSharingRepository,
            TenantAccessPolicy tenantAccessPolicy,
            FileDeletionDomainService fileDeletionDomainService,
            TenantResolverDomainService tenantResolverDomainService,
            UpdateTenantStatusDomainService updateTenantStatusDomainService,
            ApplicationEventPublisher eventPublisher
    ) {
        super(transactionManager);
        this.jpaTenantRepository = jpaTenantRepository;
        this.jpaDocumentRepository = jpaDocumentRepository;
        this.jpaApartmentSharingRepository = jpaApartmentSharingRepository;
        this.tenantAccessPolicy = tenantAccessPolicy;
        this.fileDeletionDomainService = fileDeletionDomainService;
        this.tenantResolverDomainService = tenantResolverDomainService;
        this.updateTenantStatusDomainService = updateTenantStatusDomainService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Void execute(TenantDeleteFileCommand command) {
        log.info("Starting TenantDeleteFileUseCase for fileId={}, keycloakId={}", command.fileId, command.keycloakId);
        checkTransaction();

        executeInTransaction(status -> {
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
            var savedDocument = fileDeletionDomainService.deleteFile(command.fileId, document, targetTenant, apartmentSharing, Optional.empty());
            log.info("File deletion process completed for fileId={}. Remaining document={}", command.fileId, savedDocument.isPresent());

            updateTenantStatusDomainService.updateTenantStatus(targetTenant);
            savedDocument.ifPresent(doc -> eventPublisher.publishEvent(new DocumentModifiedEvent(doc.getId())));

            return null;
        });

        return null;
    }

    public record TenantDeleteFileCommand(Long fileId, String keycloakId) {
    }

}
