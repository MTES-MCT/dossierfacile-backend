package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.service.interfaces.QueueMessageService;
import fr.dossierfacile.process.file.service.AnalyzeFileService;
import fr.dossierfacile.process.file.util.MemoryUtils;
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
public class AnalyzeFileReceiver {
    private final QueueMessageService queueMessageService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AnalyzeFileService analyzeFileService;
    @Value("${file.analysis.timeout.ms}")
    private Long fileAnalysisTimeout;

    @PostConstruct
    public void startConsumer() {
        scheduler.scheduleAtFixedRate(this::receiveFile, 0, 2, TimeUnit.SECONDS);
    }

    private void receiveFile() {
        try {
            if (!MemoryUtils.hasEnoughAvailableMemory()) {
                log.warn("There is not currently enough memory to perform consumption ");
                return;
            }
            queueMessageService.consume(QueueName.QUEUE_FILE_ANALYSIS,
                    0,
                    fileAnalysisTimeout,
                    (message) -> {
                        LoggingContext.startProcessing(message.getFileId(), ActionType.ANALYZE);
                        analyzeFileService.processFile(message.getFileId());
                        LoggingContext.endProcessing();
                    });
        } catch (Exception e) {
            log.error("Unable to consume the message queue");
        }
    }
}
