package fr.dossierfacile.api.pdfgenerator.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.DocumentService;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.common.entity.StorageFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatermarkDFDocumentConsumer {

    private final Gson gson;
    private final PdfGeneratorService pdfGeneratorService;
    private final DocumentService documentService;
    @Value("${rabbitmq.document.pdf-generation.delay}")
    private Long documentPdfGenerationDelay;

    @RabbitListener(queues = "${rabbitmq.queue.watermark-document.name}", containerFactory = "retryContainerFactory")
    public void receiveMessage(Message message) throws Exception {
        log.debug("Received message on watermark.dfdocument to process:" + message);
        Long msgTimestamp = message.getMessageProperties().getHeader("timestamp");
        delayExecution(msgTimestamp);

        try {
            Map<String, String> data = gson.fromJson(new String(message.getBody()), Map.class);
            Long documentId = Long.valueOf(data.get("id"));
            if (documentService.documentIsUpToDateAt(msgTimestamp, documentId)) {
                Long executionTimestamp = System.currentTimeMillis();
                StorageFile watermarkFile = pdfGeneratorService.generateBOPdfDocument(documentService.getDocument(documentId));
                documentService.saveWatermarkFileAt(executionTimestamp, watermarkFile, documentId);
            } else {
                log.debug("Ignore document pdf generation cause document is NOT up to date");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            throw e;
        }
    }

    private void delayExecution(Long msgTimestamp) {
        if (msgTimestamp != null) {
            long timeToWait = documentPdfGenerationDelay - (System.currentTimeMillis() - msgTimestamp);
            if (timeToWait > 0) {
                try {
                    log.info("Delayed execution in" + timeToWait + " ms");
                    Thread.sleep(timeToWait);
                } catch (InterruptedException e) {
                    log.warn("Unable to sleep the thread");
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
