package fr.gouv.bo.amqp;

import com.google.gson.Gson;
import fr.gouv.bo.amqp.model.DocumentModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class Producer {
    private final AmqpTemplate amqpTemplate;
    private final Gson gson;

    //Pdf generation
    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator}")
    private String routingKeyPdfGenerator;

    @Async
    public void generatePdf(Long documentId) {
        DocumentModel documentModel = DocumentModel.builder().id(documentId).build();
        log.info("Sending document with ID [" + documentId + "] for pdf generation");
        amqpTemplate.convertAndSend(exchangePdfGenerator, routingKeyPdfGenerator, gson.toJson(documentModel));
    }
}
