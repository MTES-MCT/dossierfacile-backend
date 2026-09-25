package fr.dossierfacile.scheduler.tasks.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.QueueEntrySource;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.model.log.DocumentLogDetails;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailedPdfDocumentCleanupService {

    /** Written in the DOCUMENT_DELETED log details to tell this deletion apart from a tenant or operator one. */
    static final String DELETION_SOURCE = "ASYNC_FAILED_PDF_GENERATION";

    private final DocumentRepository documentRepository;
    private final TenantCommonRepository tenantRepository;
    private final TenantLogCommonService tenantLogCommonService;
    private final ApartmentSharingCommonService apartmentSharingCommonService;
    private final OperatorReviewPolicy operatorReviewPolicy;
    private final ObjectMapper objectMapper;

    /**
     * Deletes the failed-PDF documents of one tenant and leaves the dossier consistent, in a dedicated
     * transaction so that a failure on one tenant does not affect the others. Entities are reloaded:
     * the candidates were selected outside any transaction and a PDF may have been generated in the meantime.
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
            deleteDocument(document, tenant);
            deleted.add(document);
        }
        if (!deleted.isEmpty()) {
            // Any document deletion invalidates the auto-validation queue entry and the full dossier PDF
            tenant.setReadyForAutoValidation(false);
            apartmentSharingCommonService.resetDossierPdfGenerated(tenant.getApartmentSharing());
            recomputeStatus(tenant);
            log.info("Tenant [{}]: {} failed-pdf documents deleted, status is now {}", tenantId, deleted.size(), tenant.getStatus());
        }
        return new FailedPdfCleanupResult(tenantId, deleted);
    }

    private void deleteDocument(Document document, Tenant tenant) {
        // Log first: DocumentLogDetails reads the document relations while the entity is still managed
        logDeletion(document, tenant);
        // Keep the in-memory model consistent: computeStatus() reads the owner's document list
        Person owner = document.getTenant() != null ? document.getTenant() : document.getGuarantor();
        owner.getDocuments().removeIf(d -> Objects.equals(d.getId(), document.getId()));
        documentRepository.delete(document);
    }

    // Same DOCUMENT_DELETED log as a tenant or operator deletion, tagged with its automatic origin
    private void logDeletion(Document document, Tenant tenant) {
        ObjectNode details = objectMapper.valueToTree(DocumentLogDetails.from(document));
        details.put("source", DELETION_SOURCE);
        tenantLogCommonService.saveTenantLog(TenantLog.builder()
                .logType(LogType.DOCUMENT_DELETED)
                .tenantId(tenant.getId())
                .logDetails(details)
                .build());
    }

    // A deletion can only make the dossier incomplete, or send a declined one back to the queue:
    // no verdict can result from it, so the verdict side effects (callbacks, mails) are not handled here
    private void recomputeStatus(Tenant tenant) {
        TenantFileStatus previous = tenant.getStatus();
        TenantFileStatus next = operatorReviewPolicy.resolveStatus(tenant, tenant.computeStatus());
        if (previous == next) {
            tenantRepository.save(tenant);
            return;
        }
        if (next == TenantFileStatus.VALIDATED || next == TenantFileStatus.DECLINED) {
            log.warn("Tenant [{}] reached {} after a document deletion, this was not expected", tenant.getId(), next);
        }
        tenant.setStatus(next);
        tenantRepository.save(tenant);
        if (next == TenantFileStatus.TO_PROCESS) {
            tenantLogCommonService.logQueueEntered(tenant.getId(), QueueEntrySource.SYSTEM_FAILURE_RECOMPUTE);
        }
    }

    /** Orphan documents (neither tenant nor guarantor): nothing to log, notify or recompute. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteOrphanDocuments(List<Long> documentIds) {
        log.warn("Deleting {} orphan documents with failed pdf: {}", documentIds.size(), documentIds);
        documentRepository.deleteAllById(documentIds);
    }
}
