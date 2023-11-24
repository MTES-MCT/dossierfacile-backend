package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.process.file.service.AnalyzeDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AnalyzeDocumentReceiver {
    private final AnalyzeDocumentService analyzeDocumentService;

    @RabbitListener(queues = "${rabbitmq.queue.document.analyze}", containerFactory = "retryContainerFactory")
    public void receiveDocument(Map<String, String> message) {
        log.debug("Received message on queue.document.analyze to process:" + message);
        Long documentId = Long.valueOf(message.get("id"));
        LoggingContext.startProcessing(documentId, ActionType.ANALYZE_DOCUMENT);
        analyzeDocumentService.processDocument(documentId);
        LoggingContext.endProcessing();
    }

}
