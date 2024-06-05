package fr.dossierfacile.api.pdfgenerator.amqp;


import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class ApartmentSharingConsumer {
    private final PdfGeneratorService pdfGeneratorService;

    @RabbitListener(queues = "${rabbitmq.queue.apartment-sharing.name}", containerFactory = "retryContainerFactory")
    public void receiveMessage(Map<String, String> item) throws IOException {
        log.info("Received full PDF for apartment sharing to process:" + item);
        try {
            pdfGeneratorService.generateFullDossierPdf(Long.valueOf(item.get("id")));
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            throw e;
        }
    }

}
