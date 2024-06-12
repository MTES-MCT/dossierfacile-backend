package fr.dossierfacile.scheduler.tasks.document;

import com.google.gson.Gson;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.scheduler.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
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

    private final AmqpTemplate amqpTemplate;
    private final Gson gson;
    private final DocumentRepository documentRepository;
    private final PartnerCallbackService partnerCallbackService;
    private final DocumentDeleteMailService documentDeleteMailService;

    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator.watermark-document}")
    private String routingKeyPdfGenerator;
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
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGenerator, messageWithId(document.getId()));
    }

    private Message messageWithId(Long id) {
        MessageProperties properties = new MessageProperties();
        return new Message(
                gson.toJson(Collections.singletonMap("id", String.valueOf(id))).getBytes(),
                properties);
    }
}