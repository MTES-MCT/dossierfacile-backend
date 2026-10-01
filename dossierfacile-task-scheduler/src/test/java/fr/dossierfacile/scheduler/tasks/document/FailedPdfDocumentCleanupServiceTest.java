package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentDeletionSource;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentDeletionCommonService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailedPdfDocumentCleanupServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private TenantCommonRepository tenantRepository;
    @Mock
    private DocumentDeletionCommonService documentDeletionCommonService;
    @Mock
    private OperatorReviewPolicy operatorReviewPolicy;
    @Mock
    private TenantLogCommonService tenantLogCommonService;

    @InjectMocks
    private FailedPdfDocumentCleanupService service;

    // The computed status is stubbed on a spy; the policy decides the persisted one
    private Tenant tenantRecomputedTo(TenantFileStatus computed, TenantFileStatus resolved) {
        Tenant tenant = spy(Tenant.builder().id(1L).status(TenantFileStatus.TO_PROCESS).build());
        doReturn(computed).when(tenant).computeStatus();
        when(operatorReviewPolicy.resolveStatus(tenant, computed)).thenReturn(resolved);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        return tenant;
    }

    @Test
    void deletesTheDocumentsThenMakesTheDossierIncomplete() {
        Tenant tenant = tenantRecomputedTo(TenantFileStatus.INCOMPLETE, TenantFileStatus.INCOMPLETE);
        Document first = Document.builder().id(10L).tenant(tenant).build();
        Document second = Document.builder().id(11L).tenant(tenant).build();
        when(documentRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(first, second));

        FailedPdfCleanupResult result = service.cleanupTenantDocuments(1L, List.of(10L, 11L));

        verify(documentDeletionCommonService).deleteDocument(first, null, DocumentDeletionSource.FAILED_PDF_CLEANUP);
        verify(documentDeletionCommonService).deleteDocument(second, null, DocumentDeletionSource.FAILED_PDF_CLEANUP);
        assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.INCOMPLETE);
        verify(tenantRepository).save(tenant);
        verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
        assertThat(result.tenantId()).isEqualTo(1L);
        assertThat(result.deletedDocuments()).containsExactly(first, second);
        assertThat(result.nothingDeleted()).isFalse();
    }

    @Test
    void enteringTheQueueAgain_isLoggedAsASystemRecomputation() {
        Tenant tenant = tenantRecomputedTo(TenantFileStatus.TO_PROCESS, TenantFileStatus.TO_PROCESS);
        tenant.setStatus(TenantFileStatus.DECLINED);
        Document declined = Document.builder().id(10L).tenant(tenant).build();
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(declined));

        service.cleanupTenantDocuments(1L, List.of(10L));

        assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
        verify(tenantLogCommonService).logQueueEntered(1L, QueueEntrySource.SYSTEM_RECOMPUTE);
    }

    @Test
    void unchangedStatus_isNotPersisted() {
        Tenant tenant = tenantRecomputedTo(TenantFileStatus.TO_PROCESS, TenantFileStatus.TO_PROCESS);
        Document document = Document.builder().id(10L).tenant(tenant).build();
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(document));

        service.cleanupTenantDocuments(1L, List.of(10L));

        verify(tenantRepository, never()).save(any());
        verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
    }

    @Test
    void skipsADocumentWhosePdfWasGeneratedMeanwhile() {
        Tenant tenant = tenantRecomputedTo(TenantFileStatus.INCOMPLETE, TenantFileStatus.INCOMPLETE);
        Document regenerated = Document.builder().id(10L).tenant(tenant).watermarkFile(new StorageFile()).build();
        Document stillFailing = Document.builder().id(11L).tenant(tenant).build();
        when(documentRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(regenerated, stillFailing));

        FailedPdfCleanupResult result = service.cleanupTenantDocuments(1L, List.of(10L, 11L));

        verify(documentDeletionCommonService, never()).deleteDocument(regenerated, null, DocumentDeletionSource.FAILED_PDF_CLEANUP);
        verify(documentDeletionCommonService).deleteDocument(stillFailing, null, DocumentDeletionSource.FAILED_PDF_CLEANUP);
        assertThat(result.deletedDocuments()).containsExactly(stillFailing);
    }

    @Test
    void doesNotRecomputeWhenEveryPdfWasGeneratedMeanwhile() {
        Tenant tenant = Tenant.builder().id(1L).status(TenantFileStatus.TO_PROCESS).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        Document regenerated = Document.builder().id(10L).tenant(tenant).watermarkFile(new StorageFile()).build();
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(regenerated));

        FailedPdfCleanupResult result = service.cleanupTenantDocuments(1L, List.of(10L));

        assertThat(result.nothingDeleted()).isTrue();
        verify(documentDeletionCommonService, never()).deleteDocument(any(), any(), any());
        verify(operatorReviewPolicy, never()).resolveStatus(any(), any());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void failsWhenTheTenantIsGone() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cleanupTenantDocuments(1L, List.of(10L)))
                .isInstanceOf(IllegalStateException.class);
        verify(documentDeletionCommonService, never()).deleteDocument(any(), any(), any());
    }

    @Test
    void deletesOrphansWithoutAnySideEffect() {
        service.deleteOrphanDocuments(List.of(20L, 21L));

        verify(documentRepository).deleteAllById(List.of(20L, 21L));
        verify(documentDeletionCommonService, never()).deleteDocument(any(), any(), any());
        verify(operatorReviewPolicy, never()).resolveStatus(any(), any());
    }
}
