package fr.dossierfacile.api.front.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class Producer {
    private final QueueMessageRepository queueMessageRepository;
    private final AmqpTemplate amqpTemplate;
    private final Gson gson;

    //File process
    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeFileProcess;
    @Value("${rabbitmq.routing.key.file.analyze}")
    private String routingKeyAnalyzeFile;
    @Value("${rabbitmq.routing.key.document.analyze}")
    private String routingKeyAnalyzeDocument;
    //Pdf generation
    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator.apartment-sharing}")
    private String routingKeyPdfGeneratorApartmentSharing;
    @Value("${rabbitmq.routing.key.pdf.generator.watermark-document}")
    private String routingKeyPdfGeneratorWatermarkDocument;


    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void generateFullPdf(Long apartmentSharingId) {
        Message msg = new Message(gson.toJson(Collections.singletonMap("id", String.valueOf( apartmentSharingId))).getBytes());
        log.info("Sending apartmentSharing with ID [" + apartmentSharingId + "] for Full PDF generation");
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGeneratorApartmentSharing, msg);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void analyzeFile(File file) {
        Long fileId = file.getId();
        log.info("Sending file with ID [{}] for analysis", fileId);
        MessageProperties properties = new MessageProperties();
        properties.setHeader("timestamp", System.currentTimeMillis());
        Message msg = new Message(gson.toJson(Collections.singletonMap("id", String.valueOf( fileId))).getBytes(), properties);
        amqpTemplate.send(exchangeFileProcess, routingKeyAnalyzeFile, msg);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendDocumentForAnalysis(Document document) {
        log.debug("Sending document with ID [{}] for analysis", document.getId());
        QueueMessage message = queueMessageRepository.findByDocumentIdAndStatus(document.getId(), QueueMessageStatus.PENDING);
        if (message == null){
            message = QueueMessage.builder()
                    .documentId(document.getId())
                    .status(QueueMessageStatus.PENDING)
                    .timestamp(System.currentTimeMillis())
                    .build();
        } else {
            message.setTimestamp(System.currentTimeMillis());
        }
        queueMessageRepository.save(message);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void sendDocumentForPdfGeneration(Document document) {
        log.debug("Sending document with ID [{}] for pdf generation", document.getId());
        MessageProperties properties = new MessageProperties();
        properties.setHeader("timestamp", System.currentTimeMillis());
        Message msg = new Message(
                gson.toJson(Collections.singletonMap("id", String.valueOf( document.getId()))).getBytes(),
                properties);
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGeneratorWatermarkDocument, msg);
    }
}
