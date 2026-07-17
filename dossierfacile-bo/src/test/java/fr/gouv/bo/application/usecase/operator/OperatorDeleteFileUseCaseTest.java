package fr.gouv.bo.application.usecase.operator;

import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.domain.service.FileDeletionDomainService;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.operator.Operator;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.TenantResolverDomainService;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.infrastructure.entity.ApartmentSharingEntity;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.OperatorEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaOperatorRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import fr.gouv.bo.domain.policy.OperatorAccessPolicy;
import fr.gouv.bo.repository.BOUserRepository;
import fr.gouv.bo.repository.DocumentRepository;
import fr.gouv.bo.security.BOApplicationAccessService;
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
    private FileDeletionDomainService fileDeletionDomainService;
    @Mock
    private JpaApartmentSharingRepository jpaApartmentSharingRepository;
    @Mock
    private JpaOperatorRepository jpaOperatorRepository;
    @Mock
    private OperatorAccessPolicy operatorAccessPolicy;
    @Mock
    private BOApplicationAccessService boApplicationAccessService;
    @Mock
    private UpdateTenantStatusDomainService updateTenantStatusDomainService;
    @Mock
    private BOUserRepository userRepository;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private OperatorDeleteFileUseCase.OperatorDeleteFileCommand command;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        command = new OperatorDeleteFileUseCase.OperatorDeleteFileCommand(100L, "operator@test.fr");

        useCase = new OperatorDeleteFileUseCase(
                transactionManager,
                jpaDocumentRepository,
                tenantResolverDomainService,
                fileDeletionDomainService,
                jpaApartmentSharingRepository,
                jpaOperatorRepository,
                operatorAccessPolicy,
                boApplicationAccessService,
                updateTenantStatusDomainService,
                userRepository,
                eventPublisher
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

        fr.dossierfacile.common.entity.BOUser legacyUser = mock(fr.dossierfacile.common.entity.BOUser.class);
        when(userRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(legacyUser));
        when(fileDeletionDomainService.deleteFile(eq(100L), eq(document), eq(tenant), eq(apartmentSharing), any())).thenReturn(Optional.of(document));
        when(updateTenantStatusDomainService.updateTenantStatus(tenant, legacyUser)).thenReturn(new UpdateTenantStatusDomainService.UpdateTenantStatusResult(false, 1L, TenantFileStatus.TO_PROCESS));

        var result = useCase.execute(command);

        assertThat(result).isNotNull();
        verify(operatorAccessPolicy).validateAccess(operator, tenant, true);
        verify(fileDeletionDomainService).deleteFile(eq(100L), eq(document), eq(tenant), eq(apartmentSharing), any());
        verify(eventPublisher).publishEvent(any(fr.dossierfacile.common.domain.event.DocumentModifiedEvent.class));
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

        fr.dossierfacile.common.entity.BOUser legacyUser = mock(fr.dossierfacile.common.entity.BOUser.class);
        when(userRepository.findByEmail("operator@test.fr")).thenReturn(Optional.of(legacyUser));
        when(fileDeletionDomainService.deleteFile(eq(100L), eq(document), eq(tenant), eq(apartmentSharing), any())).thenReturn(Optional.empty());
        when(updateTenantStatusDomainService.updateTenantStatus(tenant, legacyUser)).thenReturn(new UpdateTenantStatusDomainService.UpdateTenantStatusResult(false, 1L, TenantFileStatus.TO_PROCESS));

        var result = useCase.execute(command);

        assertThat(result).isNotNull();
        verifyNoInteractions(eventPublisher);
    }

}
