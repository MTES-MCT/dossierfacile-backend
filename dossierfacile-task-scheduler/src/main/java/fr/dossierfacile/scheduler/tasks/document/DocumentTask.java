package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.scheduler.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.dossierfacile.scheduler.tasks.TaskName.DELETE_FAILED_DOCUMENT;
import static fr.dossierfacile.scheduler.tasks.TaskName.PDF_GENERATION;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentTask {
    private final DocumentRepository documentRepository;
    private final PartnerCallbackService partnerCallbackService;
    private final DocumentDeleteMailService documentDeleteMailService;
    private final QueueMessageRepository queueMessageRepository;
    @Value("${document.pdf.failed.delay.before.delete.hours}")
    private Long delayBeforeDeleteHours;

    @Scheduled(cron = "${cron.process.pdf.generation.failed}")
    public void reLaunchFailedPDFGeneration() {
        LoggingContext.startTask(PDF_GENERATION);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime toDateTime = now.minusMinutes(30);
        List<Document> documents = documentRepository.findWithoutPDFToDate(toDateTime);
        log.info("Relaunch " + documents.size() + " failed documents to " + toDateTime);
        documents.forEach(this::sendForPDFGeneration);
        LoggingContext.endTask();
    }

    @Scheduled(cron = "${cron.delete.document.with.failed.pdf}")
    public void deleteDocumentWithFailedPdfGeneration() {
        LoggingContext.startTask(DELETE_FAILED_DOCUMENT);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime toDateTime = now.minusHours(delayBeforeDeleteHours);

        List<Document> documents = documentRepository.findDocumentWithoutPDFToDate(toDateTime);
        if (CollectionUtils.isEmpty(documents)) {
            log.info("There is not file with empty pdf");
        } else {
            Map<Tenant, List<Document>> tenantDocuments = documents.stream()
                    .collect(Collectors.groupingBy(d ->
                            Optional.ofNullable(d.getTenant())
                                    .orElseGet(() -> d.getGuarantor().getTenant())
                    ));
            tenantDocuments.forEach((tenant, docs) -> documentDeleteMailService.sendMailWithDocumentFailed(tenant.getId(), docs));
            documentRepository.deleteAll(documents);
            tenantDocuments.forEach((tenant, docs) -> partnerCallbackService.sendPartnerCallback(tenant.getId()));
        }
        LoggingContext.endTask();
    }

    private void sendForPDFGeneration(Document document) {
        log.debug("Sending document with ID [{}] for pdf generation", document.getId());
        queueMessageRepository.save(QueueMessage.builder()
                .queueName(QueueName.QUEUE_DOCUMENT_WATERMARK_PDF)
                .documentId(document.getId())
                .status(QueueMessageStatus.PENDING)
                .timestamp(System.currentTimeMillis())
                .build());
    }
}