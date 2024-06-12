package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.process.file.service.AnalyzeDocumentService;
import fr.dossierfacile.process.file.service.interfaces.DocumentService;
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
    private final DocumentService documentService;
    private final QueueMessageRepository queueMessageRepository;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    @Value("${rabbitmq.document.analyze.delay}")
    private Long documentAnalysisDelay;

    @PostConstruct
    public void startConsumer() {
        scheduler.scheduleAtFixedRate(this::receiveDocument, 0, 1, TimeUnit.SECONDS);
    }

    //@RabbitListener(queues = "${rabbitmq.queue.document.analyze}", containerFactory = "retryContainerFactory")
    private void receiveDocument() {
        QueueMessage message = queueMessageRepository.pop();
        log.debug("Received message on queue.document.analyze to process:" + message);

        if (message != null) {
            LoggingContext.startProcessing(message.getDocumentId(), ActionType.ANALYZE_DOCUMENT);
            try {
                delayExecution(message.getTimestamp());
                if (documentService.documentIsUpToDateAt(message.getTimestamp(), message.getDocumentId())) {
                    message.setStatus(QueueMessageStatus.PROCESSING);
                    queueMessageRepository.saveAndFlush(message);

                    analyzeDocumentService.processDocument(message.getDocumentId());
                    queueMessageRepository.delete(message);

                } else {
                    log.debug("Ignore document analysis because document is NOT up to date");
                    queueMessageRepository.delete(message);
                }
            } catch (Throwable t) {
                message.setStatus(QueueMessageStatus.FAILED);
                queueMessageRepository.save(message);
            }
            LoggingContext.endProcessing();
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
