package fr.dossierfacile.api.front.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.api.front.amqp.model.DocumentModel;
import fr.dossierfacile.api.front.amqp.model.TenantModel;
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

    //File process
    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeFileProcess;
    @Value("${rabbitmq.routing.key.ocr}")
    private String routingKeyOcr;

    //Pdf generation
    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator}")
    private String routingKeyPdfGenerator;

    @Async
    public void processFileOcr(Long id) {
        TenantModel tenantModel = TenantModel.builder().id(id).build();
        log.info("Send process file");
        amqpTemplate.convertAndSend(exchangeFileProcess, routingKeyOcr, gson.toJson(tenantModel));
    }

    @Async
    public void generatePdf(Long documentId, Long logId) {
        DocumentModel documentModel = DocumentModel.builder().id(documentId).logId(logId).build();
        log.info("Sending document with ID [" + documentId + "] for pdf generation");
        amqpTemplate.convertAndSend(exchangePdfGenerator, routingKeyPdfGenerator, gson.toJson(documentModel));
    }
}
