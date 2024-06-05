package fr.dossierfacile.api.pdfgenerator.amqp;


import com.google.gson.Gson;
import fr.dossierfacile.api.pdfgenerator.amqp.model.DocumentModel;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class WatermarkPdfConsumer {
    private final Gson gson;
    private final PdfGeneratorService pdfGeneratorService;

    @RabbitListener(queues = "${rabbitmq.queue.watermark-generic.name}", containerFactory = "retryContainerFactory")
    public void receiveMessage(String message) {
        log.info("Received message on watermark.generic API WM to process:" + message);
        try {
            DocumentModel documentModel = gson.fromJson(message, DocumentModel.class);
            pdfGeneratorService.processPdfGenerationFormWatermark(documentModel.getId());
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            throw e;
        }
    }

}
