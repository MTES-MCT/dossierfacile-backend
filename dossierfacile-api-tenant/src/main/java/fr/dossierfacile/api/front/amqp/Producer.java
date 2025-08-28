package fr.dossierfacile.api.front.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class Producer {
    private final QueueMessageRepository queueMessageRepository;
    private final AmqpTemplate amqpTemplate;
    private final Gson gson;
    //Pdf generation
    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.exchange.file.analysis}")
    private String exchangeFileAnalysis;
    @Value("${rabbitmq.routing.key.pdf.generator.apartment-sharing}")
    private String routingKeyPdfGeneratorApartmentSharing;
    @Value("${rabbitmq.routing.key.file.analysis}")
    private String routingKeyFileAnalysis;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void generateFullPdf(Long apartmentSharingId) {
        Message msg = new Message(gson.toJson(Collections.singletonMap("id", String.valueOf(apartmentSharingId))).getBytes());
        log.info("Sending apartmentSharing with ID [{}] for Full PDF generation", apartmentSharingId);
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGeneratorApartmentSharing, msg);
    }

    // This method is used to trigger the analysis of a file with the new Analysis service on Python with RabbitMQ
    // It sends a message to the RabbitMQ exchange with the fileId to be analysed.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void amqpAnalyseFile(Long fileId) {
        Message msg = new Message(gson.toJson(Collections.singletonMap("fileId", fileId)).getBytes());
        log.info("Sending message to analyse file with ID [{}]", fileId);
        //amqpTemplate.send(exchangeFileAnalysis, routingKeyFileAnalysis, msg);
    }

    // This method is used to trigger the analysis of a file with the old Java service
    // It saves a message in the database to be processed later by the FileAnalysisService.
    @Transactional(propagation = Propagation.SUPPORTS)
    public void analyzeFile(Long documentId, Long fileId) {
        log.info("Sending file with ID [{}] for analysis", fileId);
        queueMessageRepository.save(QueueMessage.builder()
                .queueName(QueueName.QUEUE_FILE_ANALYSIS)
                .documentId(documentId)
                .fileId(fileId)
                .status(QueueMessageStatus.PENDING)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void minifyFile(Long documentId, Long fileId) {
        log.debug("Sending file with ID [{}] for pdf generation", fileId);
        queueMessageRepository.save(QueueMessage.builder()
                .queueName(QueueName.QUEUE_FILE_MINIFY)
                .documentId(documentId)
                .fileId(fileId)
                .status(QueueMessageStatus.PENDING)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void sendDocumentForAnalysis(Document document) {
        log.info("Sending document with ID [{}] for analysis", document.getId());
        List<QueueMessage> messages = queueMessageRepository.findByQueueNameAndDocumentIdAndStatusIn(QueueName.QUEUE_DOCUMENT_ANALYSIS, document.getId(), List.of(QueueMessageStatus.PENDING));
        QueueMessage message = CollectionUtils.isNotEmpty(messages) ? messages.getFirst() : null;
        if (message == null) {
            message = QueueMessage.builder()
                    .queueName(QueueName.QUEUE_DOCUMENT_ANALYSIS)
                    .documentId(document.getId())
                    .status(QueueMessageStatus.PENDING)
                    .timestamp(System.currentTimeMillis())
                    .build();
        } else {
            message.setTimestamp(System.currentTimeMillis());
        }
        queueMessageRepository.save(message);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void sendDocumentForPdfGeneration(Document document) {
        log.debug("Sending document with ID [{}] for pdf generation", document.getId());
        queueMessageRepository.save(QueueMessage.builder()
                .queueName(QueueName.QUEUE_DOCUMENT_WATERMARK_PDF)
                .documentId(document.getId())
                .status(QueueMessageStatus.PENDING)
                .timestamp(System.currentTimeMillis())
                .build());
    }
}
