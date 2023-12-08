package fr.dossierfacile.scheduler.tasks.documenttoprocess;

import com.google.gson.Gson;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.scheduler.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static fr.dossierfacile.scheduler.tasks.TaskName.PDF_GENERATION;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentToProcessTask {

    private final AmqpTemplate amqpTemplate;
    private final Gson gson;
    private final DocumentRepository documentRepository;

    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator.watermark-document}")
    private String routingKeyPdfGenerator;

    @Scheduled(cron = "${cron.process.pdf.generation.failed}")
    public void launchFailedPDFGeneration() {
        LoggingContext.startTask(PDF_GENERATION);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime toDateTime = now.minusMinutes(10);
        documentRepository.findToProcessWithoutPDFToDate(toDateTime).forEach(this::sendForPDFGeneration);
        LoggingContext.endTask();
    }

    private void sendForPDFGeneration(Document document) {
        log.debug("Sending document with ID [{}] for pdf generation", document.getId());
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGenerator, messageWithId(document.getId()));
    }

    private Message messageWithId(Long id) {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("timestamp", System.currentTimeMillis());
        return new Message(
                gson.toJson(Collections.singletonMap("id", String.valueOf(id))).getBytes(),
                properties);
    }
}
