package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.logging.task.LogAggregator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTaskTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private PartnerCallbackService partnerCallbackService;
    @Mock
    private DocumentDeleteMailService documentDeleteMailService;
    @Mock
    private QueueMessageRepository queueMessageRepository;
    @Mock
    private FailedPdfDocumentCleanupService cleanupService;

    private DocumentTask task;

    private final Tenant tenantA = Tenant.builder().id(1L).status(TenantFileStatus.TO_PROCESS).build();
    private final Tenant tenantB = Tenant.builder().id(2L).status(TenantFileStatus.TO_PROCESS).build();

    @BeforeEach
    void setUp() {
        // Built manually to override the @Lookup method
        task = new DocumentTask(documentRepository, partnerCallbackService, documentDeleteMailService, queueMessageRepository, cleanupService) {
            @Override
            protected LogAggregator logAggregator() {
                return mock(LogAggregator.class);
            }
        };
        ReflectionTestUtils.setField(task, "delayBeforeDeleteHours", 48L);
    }

    private static FailedPdfCleanupResult deleted(Tenant tenant, Document... documents) {
        return new FailedPdfCleanupResult(tenant.getId(), List.of(documents));
    }

    @Test
    void groupsDocumentsByTenantThenNotifiesAfterEachCleanup() {
        Guarantor guarantorOfA = Guarantor.builder().id(5L).tenant(tenantA).build();
        Document directA = Document.builder().id(10L).tenant(tenantA).build();
        Document viaGuarantorA = Document.builder().id(11L).guarantor(guarantorOfA).build();
        Document directB = Document.builder().id(20L).tenant(tenantB).build();
        when(documentRepository.findDocumentWithoutPDFToDate(any(LocalDateTime.class))).thenReturn(List.of(directA, directB, viaGuarantorA));
        when(cleanupService.cleanupTenantDocuments(1L, List.of(10L, 11L))).thenReturn(deleted(tenantA, directA, viaGuarantorA));
        when(cleanupService.cleanupTenantDocuments(2L, List.of(20L))).thenReturn(deleted(tenantB, directB));

        task.deleteDocumentWithFailedPdfGeneration();

        InOrder inOrder = inOrder(cleanupService, documentDeleteMailService, partnerCallbackService);
        inOrder.verify(cleanupService).cleanupTenantDocuments(1L, List.of(10L, 11L));
        inOrder.verify(documentDeleteMailService).sendMailWithDocumentFailed(1L, List.of(directA, viaGuarantorA));
        inOrder.verify(partnerCallbackService).sendPartnerCallback(1L);
        inOrder.verify(cleanupService).cleanupTenantDocuments(2L, List.of(20L));
        inOrder.verify(documentDeleteMailService).sendMailWithDocumentFailed(2L, List.of(directB));
        inOrder.verify(partnerCallbackService).sendPartnerCallback(2L);
        verify(cleanupService, never()).deleteOrphanDocuments(anyList());
    }

    @Test
    void deletesOrphansApartWithoutNotification() {
        Document orphan = Document.builder().id(30L).build();
        Document guarantorWithoutTenant = Document.builder().id(31L).guarantor(Guarantor.builder().id(6L).build()).build();
        Document directA = Document.builder().id(10L).tenant(tenantA).build();
        when(documentRepository.findDocumentWithoutPDFToDate(any(LocalDateTime.class))).thenReturn(List.of(orphan, directA, guarantorWithoutTenant));
        when(cleanupService.cleanupTenantDocuments(1L, List.of(10L))).thenReturn(deleted(tenantA, directA));

        task.deleteDocumentWithFailedPdfGeneration();

        verify(cleanupService).deleteOrphanDocuments(List.of(30L, 31L));
        verify(cleanupService).cleanupTenantDocuments(1L, List.of(10L));
        verify(documentDeleteMailService).sendMailWithDocumentFailed(eq(1L), anyList());
        verify(partnerCallbackService).sendPartnerCallback(1L);
    }

    @Test
    void aFailingTenantDoesNotBlockTheOthers() {
        Document directA = Document.builder().id(10L).tenant(tenantA).build();
        Document directB = Document.builder().id(20L).tenant(tenantB).build();
        when(documentRepository.findDocumentWithoutPDFToDate(any(LocalDateTime.class))).thenReturn(List.of(directA, directB));
        when(cleanupService.cleanupTenantDocuments(1L, List.of(10L))).thenThrow(new IllegalStateException("boom"));
        when(cleanupService.cleanupTenantDocuments(2L, List.of(20L))).thenReturn(deleted(tenantB, directB));

        task.deleteDocumentWithFailedPdfGeneration();

        verify(documentDeleteMailService, never()).sendMailWithDocumentFailed(eq(1L), anyList());
        verify(partnerCallbackService, never()).sendPartnerCallback(1L);
        verify(documentDeleteMailService).sendMailWithDocumentFailed(2L, List.of(directB));
        verify(partnerCallbackService).sendPartnerCallback(2L);
    }

    @Test
    void doesNotNotifyWhenNothingWasDeleted() {
        Document directA = Document.builder().id(10L).tenant(tenantA).build();
        when(documentRepository.findDocumentWithoutPDFToDate(any(LocalDateTime.class))).thenReturn(List.of(directA));
        when(cleanupService.cleanupTenantDocuments(1L, List.of(10L))).thenReturn(new FailedPdfCleanupResult(1L, List.of()));

        task.deleteDocumentWithFailedPdfGeneration();

        verify(documentDeleteMailService, never()).sendMailWithDocumentFailed(anyLong(), anyList());
        verify(partnerCallbackService, never()).sendPartnerCallback(anyLong());
    }

    @Test
    void doesNothingWithoutCandidates() {
        when(documentRepository.findDocumentWithoutPDFToDate(any(LocalDateTime.class))).thenReturn(List.of());

        task.deleteDocumentWithFailedPdfGeneration();

        verify(cleanupService, never()).cleanupTenantDocuments(anyLong(), anyList());
        verify(cleanupService, never()).deleteOrphanDocuments(anyList());
    }
}
