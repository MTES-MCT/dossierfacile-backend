package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.service.interfaces.QueueMessageService;
import fr.dossierfacile.common.utils.JobContextUtil;
import fr.dossierfacile.logging.job.LogAggregator;
import fr.dossierfacile.process.file.service.AnalyzeFileService;
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
    private final LogAggregator logAggregator;
    @Value("${file.analysis.timeout.ms}")
    private Long fileAnalysisTimeout;

    @PostConstruct
    public void startConsumer() {
        scheduler.scheduleAtFixedRate(this::receiveFile, 0, 2, TimeUnit.SECONDS);
    }

    private void receiveFile() {
        try {
            queueMessageService.consume(QueueName.QUEUE_FILE_ANALYSIS,
                    0,
                    fileAnalysisTimeout,
                    (message) -> {
                        log.info("Received {} to process : {}", ActionType.ANALYZE.name(), message.getFileId());
                        analyzeFileService.processFile(message.getFileId());
                    }, (jobContext) -> {
                        log.info("Ending processing");
                        logAggregator.sendWorkerLogs(
                                jobContext.getProcessId(),
                                ActionType.ANALYZE.name(),
                                jobContext.getStartTime(),
                                JobContextUtil.prepareJobAttributes(jobContext)
                        );
                    });
        } catch (Exception e) {
            log.error("Unable to consume the message queue");
        }
    }
}
