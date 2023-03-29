package fr.dossierfacile.api.front.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.api.front.amqp.model.DocumentModel;
import fr.dossierfacile.api.front.amqp.model.TenantModel;
import fr.dossierfacile.common.FileAnalysisCriteria;
import fr.dossierfacile.common.entity.File;
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

import java.util.Map;
import java.util.TreeMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class Producer {
    private final AmqpTemplate amqpTemplate;
    private final Gson gson;

    //File process
    @Value("${rabbitmq.exchange.file.process}")
    private String exchangeFileProcess;
    @Value("${rabbitmq.routing.key.tax}")
    private String routingKeyFieProcessTax;
    @Value("${rabbitmq.routing.key.file.analyze}")
    private String routingKeyAnalyzeFile;

    //Pdf generation
    @Value("${rabbitmq.exchange.pdf.generator}")
    private String exchangePdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator.watermark-document}")
    private String routingKeyPdfGenerator;
    @Value("${rabbitmq.routing.key.pdf.generator.apartment-sharing}")
    private String routingKeyPdfGeneratorApartmentSharing;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    @Deprecated
    public void processFileTax(Long id) {
        TenantModel tenantModel = TenantModel.builder().id(id).build();
        log.info("Send process file For tenantId [" + id + "]");
        amqpTemplate.convertAndSend(exchangeFileProcess, routingKeyFieProcessTax, gson.toJson(tenantModel));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void generatePdf(Long documentId, Long logId) {
        DocumentModel documentModel = DocumentModel.builder().id(documentId).logId(logId).build();
        log.info("Sending document with ID [" + documentId + "] for pdf generation");
        amqpTemplate.convertAndSend(exchangePdfGenerator, routingKeyPdfGenerator, gson.toJson(documentModel));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void generateFullPdf(Long apartmentSharingId) {
        Message msg = messageWithId(apartmentSharingId);
        log.info("Sending apartmentSharing with ID [" + apartmentSharingId + "] for Full PDF generation");
        amqpTemplate.send(exchangePdfGenerator, routingKeyPdfGeneratorApartmentSharing, msg);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void sendFileForAnalysis(File file) {
        if (FileAnalysisCriteria.shouldBeAnalyzed(file)) {
            Long fileId = file.getId();
            log.info("Sending file with ID [{}] for analysis", fileId);
            amqpTemplate.send(exchangeFileProcess, routingKeyAnalyzeFile, messageWithId(fileId));
        }
    }

    private Message messageWithId(Long id) {
        Map<String, String> body = new TreeMap<>();
        body.putIfAbsent("id", String.valueOf(id));
        return new Message(gson.toJson(body).getBytes(), new MessageProperties());
    }

}
