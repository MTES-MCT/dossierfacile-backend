package fr.dossierfacile.api.pdfgenerator.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.api.pdfgenerator.amqp.model.DocumentModel;
import io.sentry.Sentry;
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
    private static final String EXCEPTION = "Sentry ID Exception: ";

    private final AmqpTemplate amqpTemplate;
    private final Gson gson;

    //Pdf generation
    @Value("${rabbitmq.exchange.name}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.routing.key}")
    private String routingKeyPdfGenerator;

    @Async
    public void generatePdf(Long documentId) {
        try {
            DocumentModel documentModel = DocumentModel.builder().id(documentId).build();
            log.warn("Re-Sending document with ID [" + documentId + "] to the end of the queue");
            amqpTemplate.convertAndSend(exchangePdfGenerator, routingKeyPdfGenerator, gson.toJson(documentModel));
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            throw e;
        }
    }
}
