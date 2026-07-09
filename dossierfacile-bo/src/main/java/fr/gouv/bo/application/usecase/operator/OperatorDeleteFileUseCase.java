package fr.gouv.bo.application.usecase.operator;

import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.application.service.FileDeletionProcessService;
import fr.dossierfacile.common.application.usecase.BaseUseCase;
import fr.dossierfacile.common.domain.event.DocumentModifiedEvent;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.TenantResolverDomainService;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaOperatorRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import fr.gouv.bo.domain.policy.OperatorAccessPolicy;
import fr.gouv.bo.repository.BOUserRepository;
import fr.gouv.bo.repository.DocumentRepository;
import fr.gouv.bo.security.BOApplicationAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

@Component
@Slf4j
public class OperatorDeleteFileUseCase extends BaseUseCase<OperatorDeleteFileUseCase.OperatorDeleteFileCommand, OperatorDeleteFileUseCase.OperatorDeleteFileResult> {

    private final JpaDocumentRepository jpaDocumentRepository;
    private final TenantResolverDomainService tenantResolverDomainService;
    private final FileDeletionProcessService fileDeletionProcessService;
    private final JpaApartmentSharingRepository jpaApartmentSharingRepository;
    private final JpaOperatorRepository jpaOperatorRepository;
    private final OperatorAccessPolicy operatorAccessPolicy;
    private final BOApplicationAccessService boApplicationAccessService;
    private final UpdateTenantStatusDomainService updateTenantStatusDomainService;
    private final BOUserRepository userRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public OperatorDeleteFileUseCase(
            PlatformTransactionManager transactionManager,
            JpaDocumentRepository jpaDocumentRepository,
            TenantResolverDomainService tenantResolverDomainService,
            FileDeletionProcessService fileDeletionProcessService,
            JpaApartmentSharingRepository jpaApartmentSharingRepository,
            JpaOperatorRepository jpaOperatorRepository,
            OperatorAccessPolicy operatorAccessPolicy,
            BOApplicationAccessService boApplicationAccessService,
            UpdateTenantStatusDomainService updateTenantStatusDomainService,
            BOUserRepository userRepository,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        super(transactionManager);
        this.jpaDocumentRepository = jpaDocumentRepository;
        this.tenantResolverDomainService = tenantResolverDomainService;
        this.fileDeletionProcessService = fileDeletionProcessService;
        this.jpaApartmentSharingRepository = jpaApartmentSharingRepository;
        this.jpaOperatorRepository = jpaOperatorRepository;
        this.operatorAccessPolicy = operatorAccessPolicy;
        this.boApplicationAccessService = boApplicationAccessService;
        this.updateTenantStatusDomainService = updateTenantStatusDomainService;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public OperatorDeleteFileResult execute(OperatorDeleteFileCommand command) {
        log.info("Starting OperatorDeleteFileUseCase execution for fileId={} by operator={}", command.fileId(), command.operatorEmail());
        checkTransaction();

        Optional<TransactionResult> transactionResult = executeInTransaction(status -> {

            Operator operator = jpaOperatorRepository.findByEmail(command.operatorEmail())
                    .orElseThrow(() -> new ModelNotFoundException(User.class, command.operatorEmail()));

            Document document = jpaDocumentRepository.findByFileId(command.fileId())
                    .orElseThrow(() -> new ModelNotFoundException(FileEntity.class, command.fileId()));

            Tenant targetTenant = tenantResolverDomainService.resolveTargetedTenant(document);

            ApartmentSharing apartmentSharing = jpaApartmentSharingRepository.findById(targetTenant.getApartmentSharingId())
                    .orElseThrow(() -> new ModelNotFoundException(ApartmentSharing.class, targetTenant.getApartmentSharingId()));

            var hasAccess = boApplicationAccessService.hasAccessToTenant(operator.getId(), targetTenant.getId());

            operatorAccessPolicy.validateAccess(operator, targetTenant, hasAccess);
            log.info("Access policy validation succeeded for operator on tenant id={}", targetTenant.getId());

            log.info("Calling fileDeletionProcessService for fileId={}", command.fileId());
            var savedDocument = fileDeletionProcessService.processFileDeletion(command.fileId(), document, targetTenant, apartmentSharing, Optional.of(operator));

            var userOp = userRepository.findByEmail(command.operatorEmail()).orElseThrow(() -> new ModelNotFoundException(User.class, command.operatorEmail()));
            log.info("Updating tenant status for tenant id={}", targetTenant.getId());
            var updateStatusResult = updateTenantStatusDomainService.updateTenantStatus(targetTenant, userOp);
            
            savedDocument.ifPresent(doc -> eventPublisher.publishEvent(new DocumentModifiedEvent(doc.getId())));

            return new TransactionResult(targetTenant, savedDocument.map(Document::getId), updateStatusResult);
        });

        return transactionResult.map(it -> new OperatorDeleteFileResult(it.tenant().getApartmentSharingId(), it.tenant().getId())).orElse(null);
    }

    public record OperatorDeleteFileCommand(Long fileId, String operatorEmail) {
    }

    public record OperatorDeleteFileResult(Long apartmentSharingId, Long tenantId) {
    }

    public record TransactionResult(Tenant tenant, Optional<Long> documentId,
                                    UpdateTenantStatusDomainService.UpdateTenantStatusResult updateTenantStatusResult) {
    }

}
