package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.service.interfaces.QueueMessageService;
import fr.dossierfacile.process.file.service.AnalyzeDocumentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyzeDocumentReceiver {
    private final AnalyzeDocumentService analyzeDocumentService;
    private final QueueMessageService queueMessageService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    @Value("${document.analysis.delay.ms}")
    private Long documentAnalysisDelay;
    @Value("${document.analysis.timeout.ms}")
    private Long documentAnalysisTimeout;

    @PostConstruct
    public void startConsumer() {
        scheduler.scheduleAtFixedRate(this::receiveDocument, 0, 2, TimeUnit.SECONDS);
    }

    //@RabbitListener(queues = "${rabbitmq.queue.document.analyze}", containerFactory = "retryContainerFactory")
    private void receiveDocument() {
        try {
            queueMessageService.consume(QueueName.QUEUE_DOCUMENT_ANALYSIS,
                    documentAnalysisDelay,
                    documentAnalysisTimeout,
                    (message) -> {
                        LoggingContext.startProcessing(message.getDocumentId(), ActionType.ANALYZE_DOCUMENT);
                        analyzeDocumentService.processDocument(message.getDocumentId());
                        LoggingContext.endProcessing();
                    });
        } catch (Exception e) {
            log.error("Unable to consume the message queue");
        }
    }


}
