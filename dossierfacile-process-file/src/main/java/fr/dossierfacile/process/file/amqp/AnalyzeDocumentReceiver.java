package fr.dossierfacile.process.file.amqp;

import com.google.gson.Gson;
import fr.dossierfacile.process.file.service.AnalyzeDocumentService;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyzeDocumentReceiver {
    private final AnalyzeDocumentService analyzeDocumentService;
    private final Gson gson;
    private final DocumentService documentService;
    @Value("${rabbitmq.document.analyze.delay}")
    private Long documentAnalysisDelay;

    @RabbitListener(queues = "${rabbitmq.queue.document.analyze}", containerFactory = "retryContainerFactory")
    public void receiveDocument(Message message) {
        log.debug("Received message on queue.document.analyze to process:" + message);

        Long msgTimestamp = message.getMessageProperties().getHeader("timestamp");
        delayExecution(msgTimestamp);

        Map<String, String> content = gson.fromJson(new String(message.getBody()), Map.class);
        Long documentId = Long.valueOf(content.get("id"));
        if (documentService.documentIsUpToDateAt(msgTimestamp, documentId)) {
            LoggingContext.startProcessing(documentId, ActionType.ANALYZE_DOCUMENT);
            analyzeDocumentService.processDocument(documentId);
            LoggingContext.endProcessing();
        } else {
            log.debug("Ignore document analysis because document is NOT up to date");
        }

    }

    private void delayExecution(Long msgTimestamp) {
        if (msgTimestamp != null) {
            long timeToWait = documentAnalysisDelay - (System.currentTimeMillis() - msgTimestamp);
            if (timeToWait > 0) {
                try {
                    log.debug("Delayed execution in" + timeToWait + " ms");
                    Thread.sleep(timeToWait);
                } catch (InterruptedException e) {
                    log.warn("Unable to sleep the thread");
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
