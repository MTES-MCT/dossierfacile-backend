package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.BOUser;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.SharedFileRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentDeletionCommonService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.gouv.bo.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceDocumentDeletionTest {

    private static final long OPERATOR_ID = 4L;

    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentDeletionCommonService documentDeletionCommonService;
    @Mock
    private OperatorReviewPolicy operatorReviewPolicy;
    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private MessageService messageService;
    @Mock
    private TenantLogService tenantLogService;
    @Mock
    private SharedFileRepository sharedFileRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ApartmentSharingService apartmentSharingService;

    // Constructor injection: the mocks above by type, null for the other collaborators
    @InjectMocks
    private TenantService tenantService;

    private final BOUser operator = new BOUser();
    private ApartmentSharing apartmentSharing;
    private Tenant tenant;
    private Document document;

    @BeforeEach
    void setUp() {
        operator.setId(OPERATOR_ID);
        apartmentSharing = new ApartmentSharing();
        apartmentSharing.setApplicationType(ApplicationType.ALONE);
        // The status recomputation is stubbed as unchanged: its side effects are not the point here
        tenant = spy(Tenant.builder().id(1L).status(TenantFileStatus.TO_PROCESS).readyForAutoValidation(true)
                .apartmentSharing(apartmentSharing).documents(new ArrayList<>()).build());
        doReturn(TenantFileStatus.TO_PROCESS).when(tenant).computeStatus();
        when(operatorReviewPolicy.resolveStatus(tenant, TenantFileStatus.TO_PROCESS)).thenReturn(TenantFileStatus.TO_PROCESS);
        document = Document.builder().id(10L).tenant(tenant).documentStatus(DocumentStatus.VALIDATED).build();
        tenant.getDocuments().add(document);
    }

    @Nested
    class DeleteDocument {

        @Test
        void delegatesToTheSharedDeletionThenRecomputes() {
            when(documentService.findDocumentById(10L)).thenReturn(document);
            when(documentDeletionCommonService.deleteDocument(document, OPERATOR_ID, null)).thenReturn(tenant);

            Tenant result = tenantService.deleteDocument(10L, operator);

            assertThat(result).isSameAs(tenant);
            verify(documentDeletionCommonService).deleteDocument(document, OPERATOR_ID, null);
            verify(operatorReviewPolicy).resolveStatus(tenant, TenantFileStatus.TO_PROCESS);
            verify(tenantRepository).save(tenant);
        }
    }

    @Nested
    class DeleteFile {

        private File file;

        @BeforeEach
        void setUp() {
            file = File.builder().id(7L).document(document).build();
            document.getFiles().add(file);
            when(sharedFileRepository.findById(7L)).thenReturn(Optional.of(file));
        }

        @Test
        void lastFile_deletesTheDocumentThroughTheSharedDeletion() {
            when(documentDeletionCommonService.deleteDocument(document, OPERATOR_ID, null)).thenReturn(tenant);

            tenantService.deleteFile(7L, operator);

            verify(tenantLogService).addDeleteFileLog(1L, OPERATOR_ID, file);
            verify(sharedFileRepository).delete(file);
            assertThat(document.getFiles()).isEmpty();
            verify(documentDeletionCommonService).deleteDocument(document, OPERATOR_ID, null);
            verify(documentService, never()).regeneratePdf(any());
            verify(operatorReviewPolicy).resolveStatus(tenant, TenantFileStatus.TO_PROCESS);
        }

        @Test
        void remainingFiles_sendTheDocumentBackToReviewAndResetTheAutoValidationFlag() {
            File other = File.builder().id(8L).document(document).build();
            document.getFiles().add(other);

            tenantService.deleteFile(7L, operator);

            verify(sharedFileRepository).delete(file);
            assertThat(document.getFiles()).containsExactly(other);
            assertThat(document.getDocumentStatus()).isEqualTo(DocumentStatus.TO_PROCESS);
            verify(documentRepository).save(document);
            verify(documentService).regeneratePdf(document);
            assertThat(tenant.getReadyForAutoValidation()).isFalse();
            verify(apartmentSharingService).resetDossierPdfGenerated(apartmentSharing);
            verify(documentDeletionCommonService, never()).deleteDocument(any(), any(), any());
        }
    }
}
