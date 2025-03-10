package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.common.utils.LoggerUtil;
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
import java.util.Optional;
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
    @Value("${document.pdf.failed.delay.before.delete.hours}")
    private Long delayBeforeDeleteHours;

    @Scheduled(cron = "${cron.process.pdf.generation.failed}")
    public void reLaunchFailedPDFGeneration() {
        super.startTask(PDF_GENERATION);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime toDateTime = now.minusMinutes(30);
        List<Document> documents = documentRepository.findWithoutPDFToDate(toDateTime);
        addDocumentIdListForLogging(documents);
        log.info("Relaunch {} failed documents to {}", documents.size(), toDateTime);
        documents.forEach(this::sendForPDFGeneration);
        super.endTask();
    }

    @Scheduled(cron = "${cron.delete.document.with.failed.pdf}")
    public void deleteDocumentWithFailedPdfGeneration() {
        super.startTask(DELETE_FAILED_DOCUMENT);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime toDateTime = now.minusHours(delayBeforeDeleteHours);

        List<Document> documents = documentRepository.findDocumentWithoutPDFToDate(toDateTime);
        if (CollectionUtils.isEmpty(documents)) {
            log.info("There is no file with empty pdf");
        } else {
            addDocumentIdListForLogging(documents);
            Map<Tenant, List<Document>> tenantDocuments = documents.stream()
                    .collect(Collectors.groupingBy(d ->
                            Optional.ofNullable(d.getTenant())
                                    .orElseGet(() -> d.getGuarantor().getTenant())
                    ));
            tenantDocuments.forEach((tenant, docs) -> documentDeleteMailService.sendMailWithDocumentFailed(tenant.getId(), docs));
            documentRepository.deleteAll(documents);
            tenantDocuments.forEach((tenant, docs) -> partnerCallbackService.sendPartnerCallback(tenant.getId()));
        }
        super.endTask();
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