package fr.dossierfacile.api.pdfgenerator.amqp;

import fr.dossierfacile.api.pdfgenerator.service.interfaces.DocumentService;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.service.interfaces.QueueMessageService;
import fr.dossierfacile.common.utils.JobContextUtil;
import fr.dossierfacile.logging.job.LogAggregator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatermarkDFDocumentConsumer {
    private final LogAggregator logAggregator;
    private final PdfGeneratorService pdfGeneratorService;
    private final DocumentService documentService;
    private final QueueMessageService queueMessageService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    @Value("${document.pdf-generation.delay.ms}")
    private Long documentPdfGenerationDelay;
    @Value("${document.pdf-generation.timeout.ms}")
    private Long documentPdfGenerationTimeout;

    @PostConstruct
    public void startConsumer() {
        scheduler.scheduleWithFixedDelay(this::receiveDocument, 0, 2, TimeUnit.SECONDS);
    }

    private void receiveDocument() {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            int processedCount = 0;
            boolean messageConsumed;
            do {
                messageConsumed = queueMessageService.consume(
                        QueueName.QUEUE_DOCUMENT_WATERMARK_PDF,
                        documentPdfGenerationDelay,
                        documentPdfGenerationTimeout,
                        (msg) -> {
                            long executionTimestamp = System.currentTimeMillis();
                            StorageFile watermarkFile = null;
                            try {
                                watermarkFile = pdfGeneratorService.generateBOPdfDocument(msg.getDocumentId());
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            };
                            documentService.saveWatermarkFileAt(executionTimestamp, watermarkFile, msg.getDocumentId());
                        }, (jobContext -> {
                            log.info("Ending processing");
                            logAggregator.sendWorkerLogs(
                                    jobContext.getProcessId(),
                                    ActionType.DOCUMENT_WATERMARK.name(),
                                    jobContext.getStartTime(),
                                    JobContextUtil.prepareJobAttributes(jobContext)
                            );
                        }));
                if (messageConsumed) {
                    processedCount++;
                }
            } while (messageConsumed);
            if (processedCount > 0) {
                log.info("Finished processing batch of {} messages for {}, queue is now empty 😴", processedCount, QueueName.QUEUE_DOCUMENT_WATERMARK_PDF);
            }
        } catch (Exception e) {
            log.error("Unable to consume the message queue", e);
        } finally {
            isRunning.set(false);
        }
    }
}
