package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.dossierfacile.scheduler.tasks.TaskName.DELETE_FAILED_DOCUMENT;
import static fr.dossierfacile.scheduler.tasks.TaskName.PDF_GENERATION;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentTask extends AbstractTask {
    private final DocumentRepository documentRepository;
    private final PartnerCallbackService partnerCallbackService;
    private final DocumentDeleteMailService documentDeleteMailService;
    private final QueueMessageRepository queueMessageRepository;
    private final FailedPdfDocumentCleanupService failedPdfDocumentCleanupService;
    @Value("${document.pdf.failed.delay.before.delete.hours}")
    private Long delayBeforeDeleteHours;

    @Scheduled(cron = "${cron.process.pdf.generation.failed}")
    public void reLaunchFailedPDFGeneration() {
        super.startTask(PDF_GENERATION);
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime toDateTime = now.minusMinutes(30);
            List<Document> documents = documentRepository.findWithoutPDFToDate(toDateTime);
            countDocumentIdForLogging(documents);
            log.info("Relaunch {} failed documents to {}", documents.size(), toDateTime);
            documents.forEach(this::sendForPDFGeneration);
        } catch (Exception e) {
            log.error("Error during re-launching pdf generation for failed documents", e);
        } finally {
            super.endTask();
        }
    }

    @Scheduled(cron = "${cron.delete.document.with.failed.pdf}")
    public void deleteDocumentWithFailedPdfGeneration() {
        super.startTask(DELETE_FAILED_DOCUMENT);
        try {
            LocalDateTime toDateTime = LocalDateTime.now().minusHours(delayBeforeDeleteHours);
            List<Document> documents = documentRepository.findDocumentWithoutPDFToDate(toDateTime);
            if (CollectionUtils.isEmpty(documents)) {
                log.info("There is no file with empty pdf");
                return;
            }
            countDocumentIdForLogging(documents);

            // Orphans (no tenant, no guarantor) cannot be logged nor notified: raw delete, apart
            Map<Boolean, List<Document>> byOrphan = documents.stream()
                    .collect(Collectors.partitioningBy(d -> resolveTenantId(d) == null));
            deleteOrphans(byOrphan.get(true));

            Map<Long, List<Long>> documentIdsByTenant = byOrphan.get(false).stream()
                    .collect(Collectors.groupingBy(DocumentTask::resolveTenantId,
                            Collectors.mapping(Document::getId, Collectors.toList())));
            documentIdsByTenant.forEach(this::cleanupTenant);
        } catch (Exception e) {
            log.error("Error during deleting documents with failed pdf generation", e);
        } finally {
            super.endTask();
        }
    }

    // One transaction per tenant; notifications only once it is committed
    private void cleanupTenant(Long tenantId, List<Long> documentIds) {
        try {
            FailedPdfCleanupResult result = failedPdfDocumentCleanupService.cleanupTenantDocuments(tenantId, documentIds);
            if (result.nothingDeleted()) {
                return;
            }
            documentDeleteMailService.sendMailWithDocumentFailed(tenantId, result.deletedDocuments());
            // Reloads the tenant: the callback carries the recomputed status
            partnerCallbackService.sendPartnerCallback(tenantId);
        } catch (Exception e) {
            log.error("Failed to clean up documents with failed pdf for tenant [{}]: {}", tenantId, documentIds, e);
        }
    }

    private void deleteOrphans(List<Document> orphans) {
        if (orphans.isEmpty()) {
            return;
        }
        try {
            failedPdfDocumentCleanupService.deleteOrphanDocuments(orphans.stream().map(Document::getId).toList());
        } catch (Exception e) {
            log.error("Failed to delete orphan documents with failed pdf", e);
        }
    }

    private static Long resolveTenantId(Document document) {
        if (document.getTenant() != null) {
            return document.getTenant().getId();
        }
        if (document.getGuarantor() != null && document.getGuarantor().getTenant() != null) {
            return document.getGuarantor().getTenant().getId();
        }
        return null;
    }

    private void sendForPDFGeneration(Document document) {
        log.info("Sending document with ID [{}] for pdf generation", document.getId());
        queueMessageRepository.save(QueueMessage.builder()
                .queueName(QueueName.QUEUE_DOCUMENT_WATERMARK_PDF)
                .documentId(document.getId())
                .status(QueueMessageStatus.PENDING)
                .timestamp(System.currentTimeMillis())
                .build());
    }
}
