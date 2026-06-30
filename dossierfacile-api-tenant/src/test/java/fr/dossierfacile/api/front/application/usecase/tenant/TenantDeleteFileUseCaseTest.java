package fr.dossierfacile.api.front.application.usecase.tenant;

import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.api.front.domain.policy.TenantAccessPolicy;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.FileDeletionDomainService;
import fr.dossierfacile.common.domain.service.TenantResolverDomainService;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.infrastructure.entity.ApartmentSharingEntity;
import fr.dossierfacile.common.infrastructure.entity.DocumentEntity;
import fr.dossierfacile.common.infrastructure.entity.TenantEntity;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import fr.dossierfacile.common.application.service.FileDeletionProcessService;

@ExtendWith(MockitoExtension.class)
class TenantDeleteFileUseCaseTest {

    private TenantDeleteFileUseCase useCase;

    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private JpaTenantRepository jpaTenantRepository;
    @Mock
    private JpaDocumentRepository jpaDocumentRepository;
    @Mock
    private JpaApartmentSharingRepository jpaApartmentSharingRepository;
    @Mock
    private TenantAccessPolicy tenantAccessPolicy;
    @Mock
    private FileDeletionProcessService fileDeletionProcessService;
    @Mock
    private TenantResolverDomainService tenantResolverDomainService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentIAService documentIAService;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        useCase = new TenantDeleteFileUseCase(
                transactionManager,
                jpaTenantRepository,
                jpaDocumentRepository,
                jpaApartmentSharingRepository,
                tenantAccessPolicy,
                fileDeletionProcessService,
                tenantResolverDomainService,
                documentRepository,
                documentIAService
        );
    }

    @Test
    void should_delete_file_and_trigger_document_analysis_when_document_not_deleted() {
        // Given
        String keycloakId = "keycloak-123";
        Long fileId = 100L;
        TenantDeleteFileUseCase.TenantDeleteFileCommand command =
                new TenantDeleteFileUseCase.TenantDeleteFileCommand(fileId, keycloakId);

        TenantEntity tenantEntity = TenantEntity.builder().id(1L).apartmentSharingId(10L).build();
        Tenant tenant = new Tenant(tenantEntity);

        DocumentEntity documentEntity = DocumentEntity.builder().id(50L).tenantId(1L).build();
        Document document = new Document(documentEntity);

        ApartmentSharingEntity apartmentSharingEntity = ApartmentSharingEntity.builder().id(10L).build();
        ApartmentSharing apartmentSharing = new ApartmentSharing(apartmentSharingEntity);

        when(jpaTenantRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(tenant));
        when(jpaDocumentRepository.findByFileId(fileId)).thenReturn(Optional.of(document));
        when(jpaApartmentSharingRepository.findById(10L)).thenReturn(Optional.of(apartmentSharing));
        when(tenantResolverDomainService.resolveTargetedTenant(document, tenant)).thenReturn(tenant);

        when(fileDeletionProcessService.processFileDeletion(fileId, document, tenant, apartmentSharing, Optional.empty())).thenReturn(Optional.of(document));

        fr.dossierfacile.common.entity.Document legacyDoc = mock(fr.dossierfacile.common.entity.Document.class);
        when(documentRepository.getReferenceById(50L)).thenReturn(legacyDoc);

        // When
        useCase.execute(command);

        // Then
        verify(tenantAccessPolicy).validateAccess(tenant, tenant, apartmentSharing);
        verify(fileDeletionProcessService).processFileDeletion(fileId, document, tenant, apartmentSharing, Optional.empty());
        verify(documentRepository).getReferenceById(50L);
        verify(documentIAService).analyseDocument(legacyDoc);
    }

    @Test
    void should_delete_file_and_not_analyse_when_document_is_deleted() {
        // Given
        String keycloakId = "keycloak-123";
        Long fileId = 100L;
        TenantDeleteFileUseCase.TenantDeleteFileCommand command =
                new TenantDeleteFileUseCase.TenantDeleteFileCommand(fileId, keycloakId);

        TenantEntity tenantEntity = TenantEntity.builder().id(1L).apartmentSharingId(10L).build();
        Tenant tenant = new Tenant(tenantEntity);

        DocumentEntity documentEntity = DocumentEntity.builder().id(50L).tenantId(1L).build();
        Document document = new Document(documentEntity);

        ApartmentSharingEntity apartmentSharingEntity = ApartmentSharingEntity.builder().id(10L).build();
        ApartmentSharing apartmentSharing = new ApartmentSharing(apartmentSharingEntity);

        when(jpaTenantRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(tenant));
        when(jpaDocumentRepository.findByFileId(fileId)).thenReturn(Optional.of(document));
        when(jpaApartmentSharingRepository.findById(10L)).thenReturn(Optional.of(apartmentSharing));
        when(tenantResolverDomainService.resolveTargetedTenant(document, tenant)).thenReturn(tenant);

        when(fileDeletionProcessService.processFileDeletion(fileId, document, tenant, apartmentSharing, Optional.empty())).thenReturn(Optional.empty());

        // When
        useCase.execute(command);

        // Then
        verify(tenantAccessPolicy).validateAccess(tenant, tenant, apartmentSharing);
        verify(fileDeletionProcessService).processFileDeletion(fileId, document, tenant, apartmentSharing, Optional.empty());
        verifyNoInteractions(documentRepository);
        verifyNoInteractions(documentIAService);
    }

    @Test
    void should_throw_exception_when_tenant_not_found() {
        // Given
        String keycloakId = "unknown";
        Long fileId = 100L;
        TenantDeleteFileUseCase.TenantDeleteFileCommand command =
                new TenantDeleteFileUseCase.TenantDeleteFileCommand(fileId, keycloakId);

        when(jpaTenantRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ModelNotFoundException.class);
    }
}
