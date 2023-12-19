package fr.gouv.bo.amqp;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;

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
        log.debug("Sending document with ID [{}] for pdf generation", documentId);
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGenerator, messageWithId(documentId));
    }

    private Message messageWithId(Long id) {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("timestamp", System.currentTimeMillis());
        return new Message(
                gson.toJson(Collections.singletonMap("id", String.valueOf(id))).getBytes(),
                properties);
    }


}
