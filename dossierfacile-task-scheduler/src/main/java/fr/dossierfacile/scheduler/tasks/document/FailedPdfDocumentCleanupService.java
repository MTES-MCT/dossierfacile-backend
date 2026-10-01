package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentDeletionSource;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentDeletionCommonService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailedPdfDocumentCleanupService {

    private final DocumentRepository documentRepository;
    private final TenantCommonRepository tenantRepository;
    private final DocumentDeletionCommonService documentDeletionCommonService;
    private final OperatorReviewPolicy operatorReviewPolicy;
    private final TenantLogCommonService tenantLogCommonService;

    /**
     * Deletes the failed-PDF documents of one tenant and recomputes its status
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FailedPdfCleanupResult cleanupTenantDocuments(Long tenantId, List<Long> documentIds) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant " + tenantId + " not found"));
        List<Document> deleted = new ArrayList<>();
        for (Document document : documentRepository.findAllById(documentIds)) {
            if (document.getWatermarkFile() != null) {
                log.info("Document [{}] got its PDF meanwhile, skipping", document.getId());
                continue;
            }
            documentDeletionCommonService.deleteDocument(document, null, DocumentDeletionSource.FAILED_PDF_CLEANUP);
            deleted.add(document);
        }
        if (!deleted.isEmpty()) {
            recomputeStatus(tenant);
            log.info("Tenant [{}]: {} failed-pdf documents deleted, status is now {}", tenantId, deleted.size(), tenant.getStatus());
        }
        return new FailedPdfCleanupResult(tenantId, deleted);
    }

    // Removing documents can only make a dossier incomplete, or send a declined one back to the queue:
    private void recomputeStatus(Tenant tenant) {
        TenantFileStatus previous = tenant.getStatus();
        TenantFileStatus next = operatorReviewPolicy.resolveStatus(tenant, tenant.computeStatus());
        if (previous == next) {
            return;
        }
        if (next == TenantFileStatus.VALIDATED || next == TenantFileStatus.DECLINED) {
            log.warn("Tenant [{}] reached {} after a document deletion, this was not expected", tenant.getId(), next);
        }
        tenant.setStatus(next);
        tenantRepository.save(tenant);
        if (next == TenantFileStatus.TO_PROCESS) {
            tenantLogCommonService.logQueueEntered(tenant.getId(), QueueEntrySource.SYSTEM_RECOMPUTE);
        }
    }

    /** Orphan documents (neither tenant nor guarantor): nothing to log, notify or recompute. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteOrphanDocuments(List<Long> documentIds) {
        log.warn("Deleting {} orphan documents with failed pdf: {}", documentIds.size(), documentIds);
        documentRepository.deleteAllById(documentIds);
    }
}
