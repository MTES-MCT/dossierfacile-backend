package fr.dossierfacile.api.pdfgenerator.amqp;

import com.google.gson.Gson;
import fr.dossierfacile.api.pdfgenerator.amqp.model.DocumentModel;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class Receiver {
    private static final String EXCEPTION = "Sentry ID Exception: ";

    private final Gson gson;
    private final PdfGeneratorService pdfGeneratorService;
    private final Producer producer;

    public void receiveMessage(String message) {
        try {
            DocumentModel documentModel = gson.fromJson(message, DocumentModel.class);
            try {
                pdfGeneratorService.processPdfGenerationOfDocument(documentModel.getId(), documentModel.getLogId());
            } catch (Exception e) {
                log.error(EXCEPTION + Sentry.captureException(e));
                log.error(e.getMessage(), e.getCause());
                producer.generatePdf(documentModel.getId(), documentModel.getLogId());
            }
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e));
            log.error(e.getMessage(), e.getCause());
            throw e;
        }
    }
}