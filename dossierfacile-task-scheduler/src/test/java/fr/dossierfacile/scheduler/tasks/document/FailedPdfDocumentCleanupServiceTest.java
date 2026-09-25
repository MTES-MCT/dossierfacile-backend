package fr.dossierfacile.scheduler.tasks.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
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
    private TenantLogCommonService tenantLogCommonService;
    @Mock
    private ApartmentSharingCommonService apartmentSharingCommonService;
    @Mock
    private OperatorReviewPolicy operatorReviewPolicy;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private FailedPdfDocumentCleanupService service;

    private ApartmentSharing apartmentSharing;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        service = new FailedPdfDocumentCleanupService(documentRepository, tenantRepository, tenantLogCommonService,
                apartmentSharingCommonService, operatorReviewPolicy, objectMapper);
        apartmentSharing = new ApartmentSharing();
        // The computed status is stubbed on a spy; the policy decides the persisted one
        tenant = spy(Tenant.builder().id(1L).status(TenantFileStatus.TO_PROCESS).readyForAutoValidation(true)
                .apartmentSharing(apartmentSharing).documents(new ArrayList<>()).build());
        // Not every case reaches the tenant (orphans, missing tenant): lenient on purpose
        lenient().when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
    }

    private void statusRecomputedTo(TenantFileStatus computed, TenantFileStatus resolved) {
        doReturn(computed).when(tenant).computeStatus();
        when(operatorReviewPolicy.resolveStatus(tenant, computed)).thenReturn(resolved);
    }

    private Document tenantDocument(long id) {
        Document document = Document.builder().id(id).tenant(tenant)
                .documentCategory(DocumentCategory.FINANCIAL).documentSubCategory(DocumentSubCategory.SALARY).build();
        tenant.getDocuments().add(document);
        return document;
    }

    @Test
    void deletesTheDocumentsThenLeavesTheDossierIncomplete() {
        Document first = tenantDocument(10L);
        Document second = tenantDocument(11L);
        Document kept = tenantDocument(12L);
        when(documentRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(first, second));
        statusRecomputedTo(TenantFileStatus.INCOMPLETE, TenantFileStatus.INCOMPLETE);

        FailedPdfCleanupResult result = service.cleanupTenantDocuments(1L, List.of(10L, 11L));

        verify(documentRepository).delete(first);
        verify(documentRepository).delete(second);
        assertThat(tenant.getDocuments()).containsExactly(kept);
        assertThat(tenant.getReadyForAutoValidation()).isFalse();
        assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.INCOMPLETE);
        verify(tenantRepository).save(tenant);
        verify(apartmentSharingCommonService).resetDossierPdfGenerated(apartmentSharing);
        verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
        assertThat(result.tenantId()).isEqualTo(1L);
        assertThat(result.deletedDocuments()).containsExactly(first, second);
        assertThat(result.nothingDeleted()).isFalse();
    }

    @Test
    void logsEachDeletionAsAutomaticBeforeDeleting() {
        Document document = tenantDocument(10L);
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(document));
        statusRecomputedTo(TenantFileStatus.INCOMPLETE, TenantFileStatus.INCOMPLETE);

        service.cleanupTenantDocuments(1L, List.of(10L));

        ArgumentCaptor<TenantLog> captor = ArgumentCaptor.forClass(TenantLog.class);
        InOrder inOrder = inOrder(tenantLogCommonService, documentRepository);
        inOrder.verify(tenantLogCommonService).saveTenantLog(captor.capture());
        inOrder.verify(documentRepository).delete(document);
        TenantLog log = captor.getValue();
        assertThat(log.getLogType()).isEqualTo(LogType.DOCUMENT_DELETED);
        assertThat(log.getTenantId()).isEqualTo(1L);
        assertThat(log.getOperatorId()).isNull();
        assertThat(log.getLogDetails()).hasToString("""
                {"documentCategory":"FINANCIAL","documentSubCategory":"SALARY","tenantId":1,"documentId":10,"source":"ASYNC_FAILED_PDF_GENERATION"}""");
    }

    @Test
    void guarantorDocument_isRemovedFromTheGuarantorAndLoggedOnTheTenant() {
        Guarantor guarantor = Guarantor.builder().id(5L).tenant(tenant).documents(new ArrayList<>()).build();
        Document document = Document.builder().id(20L).guarantor(guarantor)
                .documentCategory(DocumentCategory.IDENTIFICATION).documentSubCategory(DocumentSubCategory.FRENCH_PASSPORT).build();
        guarantor.getDocuments().add(document);
        when(documentRepository.findAllById(List.of(20L))).thenReturn(List.of(document));
        statusRecomputedTo(TenantFileStatus.INCOMPLETE, TenantFileStatus.INCOMPLETE);

        service.cleanupTenantDocuments(1L, List.of(20L));

        assertThat(guarantor.getDocuments()).isEmpty();
        verify(documentRepository).delete(document);
        ArgumentCaptor<TenantLog> captor = ArgumentCaptor.forClass(TenantLog.class);
        verify(tenantLogCommonService).saveTenantLog(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getLogDetails().get("guarantorId").asLong()).isEqualTo(5L);
    }

    @Test
    void enteringTheQueueAgain_isLoggedAsASystemRecomputation() {
        tenant.setStatus(TenantFileStatus.DECLINED);
        Document document = tenantDocument(10L);
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(document));
        statusRecomputedTo(TenantFileStatus.TO_PROCESS, TenantFileStatus.TO_PROCESS);

        service.cleanupTenantDocuments(1L, List.of(10L));

        assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
        verify(tenantLogCommonService).logQueueEntered(1L, QueueEntrySource.SYSTEM_FAILURE_RECOMPUTE);
    }

    @Test
    void unchangedStatus_stillPersistsTheFlagReset() {
        Document document = tenantDocument(10L);
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(document));
        statusRecomputedTo(TenantFileStatus.TO_PROCESS, TenantFileStatus.TO_PROCESS);

        service.cleanupTenantDocuments(1L, List.of(10L));

        assertThat(tenant.getStatus()).isEqualTo(TenantFileStatus.TO_PROCESS);
        assertThat(tenant.getReadyForAutoValidation()).isFalse();
        verify(tenantRepository).save(tenant);
        verify(tenantLogCommonService, never()).logQueueEntered(anyLong(), any());
    }

    @Test
    void skipsADocumentWhosePdfWasGeneratedMeanwhile() {
        Document regenerated = tenantDocument(10L);
        regenerated.setWatermarkFile(new StorageFile());
        Document stillFailing = tenantDocument(11L);
        when(documentRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(regenerated, stillFailing));
        statusRecomputedTo(TenantFileStatus.INCOMPLETE, TenantFileStatus.INCOMPLETE);

        FailedPdfCleanupResult result = service.cleanupTenantDocuments(1L, List.of(10L, 11L));

        verify(documentRepository, never()).delete(regenerated);
        verify(documentRepository).delete(stillFailing);
        assertThat(tenant.getDocuments()).containsExactly(regenerated);
        assertThat(result.deletedDocuments()).containsExactly(stillFailing);
    }

    @Test
    void doesNothingWhenEveryPdfWasGeneratedMeanwhile() {
        Document regenerated = tenantDocument(10L);
        regenerated.setWatermarkFile(new StorageFile());
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(regenerated));

        FailedPdfCleanupResult result = service.cleanupTenantDocuments(1L, List.of(10L));

        assertThat(result.nothingDeleted()).isTrue();
        assertThat(tenant.getReadyForAutoValidation()).isTrue();
        verify(documentRepository, never()).delete(any());
        verify(tenantLogCommonService, never()).saveTenantLog(any());
        verify(operatorReviewPolicy, never()).resolveStatus(any(), any());
        verify(tenantRepository, never()).save(any());
        verify(apartmentSharingCommonService, never()).resetDossierPdfGenerated(any());
    }

    @Test
    void failsWhenTheTenantIsGone() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cleanupTenantDocuments(1L, List.of(10L)))
                .isInstanceOf(IllegalStateException.class);
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void deletesOrphansWithoutAnySideEffect() {
        service.deleteOrphanDocuments(List.of(20L, 21L));

        verify(documentRepository).deleteAllById(List.of(20L, 21L));
        verify(tenantLogCommonService, never()).saveTenantLog(any());
        verify(operatorReviewPolicy, never()).resolveStatus(any(), any());
    }
}
