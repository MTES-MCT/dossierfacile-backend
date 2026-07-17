package fr.dossierfacile.common.infrastructure.messaging;

import com.google.gson.Gson;
import fr.dossierfacile.common.domain.service.MessagePublisher;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class AmqpProducer implements MessagePublisher {

    private final QueueMessageRepository queueMessageRepository;
    private final AmqpTemplate amqpTemplate;
    private final Gson gson;

    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;

    @Value("${rabbitmq.routing.key.pdf.generator.apartment-sharing}")
    private String routingKeyPdfGeneratorApartmentSharing;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void generateFullPdf(Long apartmentSharingId) {
        Message msg = new Message(gson.toJson(Collections.singletonMap("id", String.valueOf(apartmentSharingId))).getBytes());
        log.info("Sending apartmentSharing with ID [{}] for Full PDF generation", apartmentSharingId);
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGeneratorApartmentSharing, msg);
    }

    @Override
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

    @Override
    public void sendDocumentForPdfGeneration(Long documentId) {
        log.debug("Sending document with ID [{}] for pdf generation", documentId);
        queueMessageRepository.save(QueueMessage.builder()
                .queueName(QueueName.QUEUE_DOCUMENT_WATERMARK_PDF)
                .documentId(documentId)
                .status(QueueMessageStatus.PENDING)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void sendDocumentForPdfGeneration(Document document) {
        sendDocumentForPdfGeneration(document.getId());
    }
}
