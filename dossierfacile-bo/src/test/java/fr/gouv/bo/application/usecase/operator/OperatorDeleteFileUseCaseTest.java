package fr.gouv.bo.application.usecase.operator;

import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.application.service.FileDeletionProcessService;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.TenantResolverDomainService;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService;
import fr.dossierfacile.common.dto.mail.ApartmentSharingDto;
import fr.dossierfacile.common.dto.mail.TenantDto;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.PartnerCallBackType;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.infrastructure.entity.ApartmentSharingEntity;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.OperatorEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperatorDeleteFileUseCaseTest {

    private OperatorDeleteFileUseCase useCase;

    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private JpaDocumentRepository jpaDocumentRepository;
    @Mock
    private TenantResolverDomainService tenantResolverDomainService;
    @Mock
    private FileDeletionProcessService fileDeletionProcessService;
    @Mock
    private JpaApartmentSharingRepository jpaApartmentSharingRepository;
    @Mock
    private JpaOperatorRepository jpaOperatorRepository;
    @Mock
    private OperatorAccessPolicy operatorAccessPolicy;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private BOApplicationAccessService boApplicationAccessService;
    @Mock
    private DocumentIAService documentIAService;
    @Mock
    private UpdateTenantStatusDomainService updateTenantStatusDomainService;
    @Mock
    private TenantCommonRepository tenantCommonRepository;
    @Mock
    private BOUserRepository userRepository;
    @Mock
    private MessageService messageService;
    @Mock
    private TenantLogCommonService tenantLogCommonService;
    @Mock
    private TenantCommonService tenantCommonService;
    @Mock
    private OperatorLogRepository operatorLogRepository;
    @Mock
    private TenantMapperForMail tenantMapperForMail;
    @Mock
    private ApartmentSharingMapperForMail apartmentSharingMapperForMail;
    @Mock
    private PartnerCallBackService partnerCallBackService;
    @Mock
    private MailService mailService;

    private OperatorDeleteFileUseCase.OperatorDeleteFileCommand command;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        command = new OperatorDeleteFileUseCase.OperatorDeleteFileCommand(100L, "operator@test.fr");

        useCase = new OperatorDeleteFileUseCase(
                transactionManager,
                jpaDocumentRepository,
                tenantResolverDomainService,
                fileDeletionProcessService,
                jpaApartmentSharingRepository,
                jpaOperatorRepository,
                operatorAccessPolicy,
                documentRepository,
                boApplicationAccessService,
                documentIAService,
                updateTenantStatusDomainService,
                tenantCommonRepository,
                userRepository,
                messageService,
                tenantLogCommonService,
                tenantCommonService,
                operatorLogRepository,
                tenantMapperForMail,
                apartmentSharingMapperForMail,
                partnerCallBackService,
                mailService
        );
    }

    @Test
    @DisplayName("Should throw ModelNotFoundException when operator not found")
    void should_throw_exception_when_operator_not_found() {
        when(jpaOperatorRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ModelNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ModelNotFoundException when document not found")
    void should_throw_exception_when_document_not_found() {
        Operator operator = new Operator(OperatorEntity.builder().id(999L).email("operator@test.fr").build());
        when(jpaOperatorRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(operator));
        when(jpaDocumentRepository.findByFileId(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ModelNotFoundException.class);
    }

    @Test
    @DisplayName("Should validate access and delete file successfully when document is not fully deleted and no status changed")
    void should_delete_file_successfully_without_status_change() {
        Operator operator = new Operator(OperatorEntity.builder().id(999L).email("operator@test.fr").build());
        Document document = new Document(DocumentEntity.builder().id(50L).tenantId(1L).build());
        Tenant tenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(ApartmentSharingEntity.builder().id(10L).build());

        when(jpaOperatorRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(operator));
        when(jpaDocumentRepository.findByFileId(100L)).thenReturn(Optional.of(document));
        when(tenantResolverDomainService.resolveTargetedTenant(document)).thenReturn(tenant);
        when(jpaApartmentSharingRepository.findById(10L)).thenReturn(Optional.of(apartmentSharing));
        when(boApplicationAccessService.hasAccessToTenant(999L, 1L)).thenReturn(true);

        when(fileDeletionProcessService.processFileDeletion(eq(100L), eq(document), eq(tenant), eq(apartmentSharing), any())).thenReturn(Optional.of(document));
        when(updateTenantStatusDomainService.updateTenantStatus(tenant)).thenReturn(new UpdateTenantStatusDomainService.UpdateTenantStatusResult(false, 1L, TenantFileStatus.TO_PROCESS));

        fr.dossierfacile.common.entity.Document legacyDoc = mock(fr.dossierfacile.common.entity.Document.class);
        when(documentRepository.getReferenceById(50L)).thenReturn(legacyDoc);

        var result = useCase.execute(command);

        assertThat(result).isNotNull();
        verify(operatorAccessPolicy).validateAccess(operator, tenant, true);
        verify(fileDeletionProcessService).processFileDeletion(eq(100L), eq(document), eq(tenant), eq(apartmentSharing), any());
        verify(documentIAService).analyseDocument(legacyDoc);
    }

    @Test
    @DisplayName("Should validate access and delete file successfully and not trigger IA when document is completely deleted")
    void should_delete_file_successfully_and_not_analyse_ia_when_document_deleted() {
        Operator operator = new Operator(OperatorEntity.builder().id(999L).email("operator@test.fr").build());
        Document document = new Document(DocumentEntity.builder().id(50L).tenantId(1L).build());
        Tenant tenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(ApartmentSharingEntity.builder().id(10L).build());

        when(jpaOperatorRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(operator));
        when(jpaDocumentRepository.findByFileId(100L)).thenReturn(Optional.of(document));
        when(tenantResolverDomainService.resolveTargetedTenant(document)).thenReturn(tenant);
        when(jpaApartmentSharingRepository.findById(10L)).thenReturn(Optional.of(apartmentSharing));
        when(boApplicationAccessService.hasAccessToTenant(999L, 1L)).thenReturn(true);

        when(fileDeletionProcessService.processFileDeletion(eq(100L), eq(document), eq(tenant), eq(apartmentSharing), any())).thenReturn(Optional.empty());
        when(updateTenantStatusDomainService.updateTenantStatus(tenant)).thenReturn(new UpdateTenantStatusDomainService.UpdateTenantStatusResult(false, 1L, TenantFileStatus.TO_PROCESS));

        var result = useCase.execute(command);

        assertThat(result).isNotNull();
        verifyNoInteractions(documentRepository);
        verifyNoInteractions(documentIAService);
    }

    @Test
    @DisplayName("Should execute legacy transitions when status changed to VALIDATED")
    void should_execute_validated_legacy_transitions() {
        Operator operator = new Operator(OperatorEntity.builder().id(999L).email("operator@test.fr").build());
        Document document = new Document(DocumentEntity.builder().id(50L).tenantId(1L).build());
        Tenant tenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(ApartmentSharingEntity.builder().id(10L).build());

        when(jpaOperatorRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(operator));
        when(jpaDocumentRepository.findByFileId(100L)).thenReturn(Optional.of(document));
        when(tenantResolverDomainService.resolveTargetedTenant(document)).thenReturn(tenant);
        when(jpaApartmentSharingRepository.findById(10L)).thenReturn(Optional.of(apartmentSharing));
        when(boApplicationAccessService.hasAccessToTenant(999L, 1L)).thenReturn(true);

        when(fileDeletionProcessService.processFileDeletion(eq(100L), eq(document), eq(tenant), eq(apartmentSharing), any())).thenReturn(Optional.empty());
        when(updateTenantStatusDomainService.updateTenantStatus(tenant)).thenReturn(new UpdateTenantStatusDomainService.UpdateTenantStatusResult(true, 1L, TenantFileStatus.VALIDATED));

        fr.dossierfacile.common.entity.Tenant legacyTenant = mock(fr.dossierfacile.common.entity.Tenant.class);
        fr.dossierfacile.common.entity.BOUser legacyUser = mock(fr.dossierfacile.common.entity.BOUser.class);
        when(tenantCommonRepository.getReferenceById(1L)).thenReturn(legacyTenant);
        when(userRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(legacyUser));

        useCase.execute(command);

        verify(tenantCommonService).changeTenantStatusToValidated(legacyTenant);
        verify(messageService).markReadAdmin(legacyTenant);
        verify(tenantLogCommonService).saveTenantLog(any(TenantLog.class));
        verify(operatorLogRepository).save(any(OperatorLog.class));
    }

    @Test
    @DisplayName("Should execute legacy transitions, callback and couple emails when status changed to DECLINED")
    void should_execute_declined_legacy_transitions_for_couple() {
        Operator operator = new Operator(OperatorEntity.builder().id(999L).email("operator@test.fr").build());
        Document document = new Document(DocumentEntity.builder().id(50L).tenantId(1L).build());
        Tenant tenant = new Tenant(TenantEntity.builder().id(1L).apartmentSharingId(10L).build());
        ApartmentSharing apartmentSharing = new ApartmentSharing(ApartmentSharingEntity.builder().id(10L).build());

        when(jpaOperatorRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(operator));
        when(jpaDocumentRepository.findByFileId(100L)).thenReturn(Optional.of(document));
        when(tenantResolverDomainService.resolveTargetedTenant(document)).thenReturn(tenant);
        when(jpaApartmentSharingRepository.findById(10L)).thenReturn(Optional.of(apartmentSharing));
        when(boApplicationAccessService.hasAccessToTenant(999L, 1L)).thenReturn(true);

        when(fileDeletionProcessService.processFileDeletion(eq(100L), eq(document), eq(tenant), eq(apartmentSharing), any())).thenReturn(Optional.empty());
        when(updateTenantStatusDomainService.updateTenantStatus(tenant)).thenReturn(new UpdateTenantStatusDomainService.UpdateTenantStatusResult(true, 1L, TenantFileStatus.DECLINED));

        fr.dossierfacile.common.entity.Tenant legacyTenant = mock(fr.dossierfacile.common.entity.Tenant.class);
        fr.dossierfacile.common.entity.BOUser legacyUser = mock(fr.dossierfacile.common.entity.BOUser.class);
        when(tenantCommonRepository.getReferenceById(1L)).thenReturn(legacyTenant);
        when(userRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(legacyUser));

        TenantDto tenantDto = mock(TenantDto.class);
        ApartmentSharingDto apartmentSharingDto = mock(ApartmentSharingDto.class);
        TenantDto coTenantDto = mock(TenantDto.class);
        when(coTenantDto.getEmail()).thenReturn("cotenant@test.fr");

        when(tenantMapperForMail.toDto(legacyTenant)).thenReturn(tenantDto);
        fr.dossierfacile.common.entity.ApartmentSharing legacyApartmentSharing = mock(fr.dossierfacile.common.entity.ApartmentSharing.class);
        when(legacyTenant.getApartmentSharing()).thenReturn(legacyApartmentSharing);
        when(apartmentSharingMapperForMail.toDto(legacyApartmentSharing)).thenReturn(apartmentSharingDto);
        when(apartmentSharingDto.getApplicationType()).thenReturn(ApplicationType.COUPLE);
        when(apartmentSharingDto.getTenants()).thenReturn(List.of(coTenantDto));

        useCase.execute(command);

        verify(messageService).markReadAdmin(legacyTenant);
        verify(tenantLogCommonService).saveTenantLog(any(TenantLog.class));
        verify(operatorLogRepository).save(any(OperatorLog.class));
        verify(partnerCallBackService).sendCallBack(legacyTenant, PartnerCallBackType.DENIED_ACCOUNT);
        verify(mailService).sendEmailToTenantAfterTenantDenied(coTenantDto, tenantDto, null);
    }
}
