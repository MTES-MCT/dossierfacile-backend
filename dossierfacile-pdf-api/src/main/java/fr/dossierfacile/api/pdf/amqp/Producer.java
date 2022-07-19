package fr.dossierfacile.api.pdf.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.api.pdf.amqp.model.DocumentModel;
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
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Async
    public void generatePdf(Long documentId, Long logId) {
        DocumentModel documentModel = DocumentModel.builder().id(documentId).logId(logId).build();
        log.info("Sending document with ID [" + documentId + "] for pdf generation");
        amqpTemplate.convertAndSend(exchangeName, routingKey, gson.toJson(documentModel));
    }

}
