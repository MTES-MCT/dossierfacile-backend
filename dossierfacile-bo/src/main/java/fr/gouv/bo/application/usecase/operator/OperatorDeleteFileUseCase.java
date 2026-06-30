package fr.gouv.bo.application.usecase.operator;

import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.application.service.FileDeletionProcessService;
import fr.dossierfacile.common.application.usecase.BaseUseCase;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.TenantResolverDomainService;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService;
import fr.dossierfacile.common.dto.mail.ApartmentSharingDto;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.ActionOperatorType;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaOperatorRepository;
import fr.dossierfacile.common.mapper.mail.ApartmentSharingMapperForMail;
import fr.dossierfacile.common.mapper.mail.TenantMapperForMail;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import fr.dossierfacile.common.service.interfaces.TenantCommonService;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import fr.gouv.bo.domain.policy.OperatorAccessPolicy;
import fr.gouv.bo.repository.BOUserRepository;
import fr.gouv.bo.repository.DocumentRepository;
import fr.gouv.bo.repository.OperatorLogRepository;
import fr.gouv.bo.security.BOApplicationAccessService;
import fr.gouv.bo.security.UserPrincipal;
import fr.gouv.bo.service.MailService;
import fr.gouv.bo.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@Slf4j
public class OperatorDeleteFileUseCase extends BaseUseCase<OperatorDeleteFileUseCase.OperatorDeleteFileCommand, OperatorDeleteFileUseCase.OperatorDeleteFileResult> {

    private final JpaDocumentRepository jpaDocumentRepository;
    private final TenantResolverDomainService tenantResolverDomainService;
    private final FileDeletionProcessService fileDeletionProcessService;
    private final JpaApartmentSharingRepository jpaApartmentSharingRepository;
    private final JpaOperatorRepository jpaOperatorRepository;
    private final OperatorAccessPolicy operatorAccessPolicy;
    private final DocumentRepository documentRepository;
    private final BOApplicationAccessService boApplicationAccessService;
    private final DocumentIAService documentIAService;
    private final UpdateTenantStatusDomainService updateTenantStatusDomainService;
    private final TenantCommonRepository tenantCommonRepository;
    private final BOUserRepository userRepository;
    private final MessageService messageService;
    private final TenantLogCommonService tenantLogCommonService;
    private final TenantCommonService tenantCommonService;
    private final OperatorLogRepository operatorLogRepository;
    private final TenantMapperForMail tenantMapperForMail;
    private final ApartmentSharingMapperForMail apartmentSharingMapperForMail;
    private final PartnerCallBackService partnerCallBackService;
    private final MailService mailService;

    public OperatorDeleteFileUseCase(
            PlatformTransactionManager transactionManager,
            JpaDocumentRepository jpaDocumentRepository,
            TenantResolverDomainService tenantResolverDomainService,
            FileDeletionProcessService fileDeletionProcessService,
            JpaApartmentSharingRepository jpaApartmentSharingRepository,
            JpaOperatorRepository jpaOperatorRepository,
            OperatorAccessPolicy operatorAccessPolicy,
            DocumentRepository documentRepository,
            BOApplicationAccessService boApplicationAccessService,
            DocumentIAService documentIAService,
            UpdateTenantStatusDomainService updateTenantStatusDomainService,
            TenantCommonRepository tenantCommonRepository,
            BOUserRepository userRepository,
            MessageService messageService,
            TenantLogCommonService tenantLogCommonService, TenantCommonService tenantCommonService, OperatorLogRepository operatorLogRepository, TenantMapperForMail tenantMapperForMail, ApartmentSharingMapperForMail apartmentSharingMapperForMail, PartnerCallBackService partnerCallBackService, MailService mailService) {
        super(transactionManager);
        this.jpaDocumentRepository = jpaDocumentRepository;
        this.tenantResolverDomainService = tenantResolverDomainService;
        this.fileDeletionProcessService = fileDeletionProcessService;
        this.jpaApartmentSharingRepository = jpaApartmentSharingRepository;
        this.jpaOperatorRepository = jpaOperatorRepository;
        this.operatorAccessPolicy = operatorAccessPolicy;
        this.documentRepository = documentRepository;
        this.boApplicationAccessService = boApplicationAccessService;
        this.documentIAService = documentIAService;
        this.updateTenantStatusDomainService = updateTenantStatusDomainService;
        this.tenantCommonRepository = tenantCommonRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.tenantLogCommonService = tenantLogCommonService;
        this.tenantCommonService = tenantCommonService;
        this.operatorLogRepository = operatorLogRepository;
        this.tenantMapperForMail = tenantMapperForMail;
        this.apartmentSharingMapperForMail = apartmentSharingMapperForMail;
        this.partnerCallBackService = partnerCallBackService;
        this.mailService = mailService;
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

            log.info("Updating tenant status for tenant id={}", targetTenant.getId());
            var updateStatusResult = updateTenantStatusDomainService.updateTenantStatus(targetTenant);

            return new TransactionResult(targetTenant, savedDocument.map(Document::getId), updateStatusResult);
        });

        // TODO : Il faut supprimer ce traitement quand la migration vers les entité DDD sera fini !
        if (transactionResult.isPresent()) {
            var value = transactionResult.get();
            log.info("Checking legacy transition status for tenant id={} (hasBeenUpdated={})", value.tenant.getId(), value.updateTenantStatusResult.hasBeenUpdated());
            Optional<TenantStatusDeclinedResult> result = executeInTransaction(status -> {
                if (value.updateTenantStatusResult.hasBeenUpdated()) {
                    var tenant = tenantCommonRepository.getReferenceById(value.tenant.getId());
                    var userOp = userRepository.findByEmail(command.operatorEmail()).orElseThrow(() -> new ModelNotFoundException(User.class, command.operatorEmail()));
                    log.info("Transitioning tenant id={} status to: {}", tenant.getId(), value.updateTenantStatusResult.newStatus());
                    switch (value.updateTenantStatusResult.newStatus()) {
                        case VALIDATED -> changeTenantStatusToValidated(tenant, userOp);
                        case DECLINED -> {
                            changeTenantStatusToDeclined(tenant, userOp);
                            var tenantDto = tenantMapperForMail.toDto(tenant);
                            var apartmentSharingDto = apartmentSharingMapperForMail.toDto(tenant.getApartmentSharing());
                            return new TenantStatusDeclinedResult(
                                    tenant,
                                    tenantDto,
                                    apartmentSharingDto
                             );
                        }
                    }
                }
                return null;
            });

            result.ifPresent(it -> {
                log.info("Sending callbacks and notifications for declined tenant id={}", it.tenant.getId());
                sendCallbackTenantDeclined(it.tenant, it.tenantDto, it.apartmentSharingDto);
            });
        }

        transactionResult.flatMap(it -> it.documentId).ifPresent(
                documentId -> transactionTemplate.execute(status -> {
                    log.info("Triggering DocumentIA analysis after commit for documentId={}", documentId);
                    var document = documentRepository.getReferenceById(documentId);
                    documentIAService.analyseDocument(document);
                    return null;
                })
        );

        return transactionResult.map(it -> new OperatorDeleteFileResult(it.tenant().getApartmentSharingId(), it.tenant().getId())).orElse(null);
    }

    private void changeTenantStatusToValidated(fr.dossierfacile.common.entity.Tenant tenant, User operator) {
        tenantCommonService.changeTenantStatusToValidated(tenant);
        messageService.markReadAdmin(tenant);
        tenantLogCommonService.saveTenantLog(new TenantLog(LogType.ACCOUNT_VALIDATED, tenant.getId(), operator.getId()));
        operatorLogRepository.save(new OperatorLog(tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS, 1, null));
    }

    private void changeTenantStatusToDeclined(fr.dossierfacile.common.entity.Tenant tenant, User operator) {
        messageService.markReadAdmin(tenant);

        tenantLogCommonService.saveTenantLog(new TenantLog(LogType.ACCOUNT_DENIED, tenant.getId(), operator.getId(), null));
        operatorLogRepository.save(new OperatorLog(
                tenant, operator, tenant.getStatus(), ActionOperatorType.STOP_PROCESS, 1, null
        ));
    }

    private void sendCallbackTenantDeclined(fr.dossierfacile.common.entity.Tenant tenant, TenantDto tenantDto, ApartmentSharingDto apartmentSharingDto) {
        partnerCallBackService.sendCallBack(tenant, PartnerCallBackType.DENIED_ACCOUNT);

        if (apartmentSharingDto.getApplicationType() == ApplicationType.COUPLE) {
            apartmentSharingDto.getTenants().stream()
                    .filter(t -> isNotBlank(t.getEmail()))
                    .forEach(t -> mailService.sendEmailToTenantAfterTenantDenied(t, tenantDto, null));
        } else {
            mailService.sendMailNotificationAfterDeny(tenantDto, null);
        }
    }

    public record OperatorDeleteFileCommand(Long fileId, String operatorEmail) {
    }

    public record OperatorDeleteFileResult(Long apartmentSharingId, Long tenantId) {
    }

    public record TransactionResult(Tenant tenant, Optional<Long> documentId,
                                    UpdateTenantStatusDomainService.UpdateTenantStatusResult updateTenantStatusResult) {
    }

    public record TenantStatusDeclinedResult(fr.dossierfacile.common.entity.Tenant tenant, TenantDto tenantDto,
                                             ApartmentSharingDto apartmentSharingDto) {

    }

}
