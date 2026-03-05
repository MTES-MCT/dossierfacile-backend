package fr.dossierfacile.api.front.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
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
    //Pdf generation
    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator.apartment-sharing}")
    private String routingKeyPdfGeneratorApartmentSharing;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void generateFullPdf(Long apartmentSharingId) {
        Message msg = new Message(gson.toJson(Collections.singletonMap("id", String.valueOf(apartmentSharingId))).getBytes());
        log.info("Sending apartmentSharing with ID [{}] for Full PDF generation", apartmentSharingId);
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGeneratorApartmentSharing, msg);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void processFile(Long documentId, Long fileId) {
        log.debug("Sending file with ID [{}] for processing ", fileId);
        queueMessageRepository.save(QueueMessage.builder()
                .queueName(QueueName.QUEUE_FILE_PROCESSING)
                .documentId(documentId)
                .fileId(fileId)
                .status(QueueMessageStatus.PENDING)
                .timestamp(System.currentTimeMillis())
                .build());
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
