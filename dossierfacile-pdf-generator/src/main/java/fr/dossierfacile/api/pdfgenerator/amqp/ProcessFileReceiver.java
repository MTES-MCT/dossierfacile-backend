package fr.dossierfacile.api.pdfgenerator.amqp;

import fr.dossierfacile.api.pdfgenerator.service.interfaces.ProcessFileService;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.service.interfaces.QueueMessageService;
import fr.dossierfacile.common.utils.JobContextUtil;
import fr.dossierfacile.logging.job.LogAggregator;
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
public class ProcessFileReceiver {

    private final QueueMessageService queueMessageService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final LogAggregator logAggregator;
    private final ProcessFileService processFileService;

    @Value("${file.minify.timeout.ms:40000}")
    private Long fileMinifyTimeout;

    @PostConstruct
    public void startConsumer() {
        scheduler.scheduleAtFixedRate(this::receiveFile, 0, 2, TimeUnit.SECONDS);
    }

    private void receiveFile() {
        try {
            queueMessageService.consume(QueueName.QUEUE_FILE_PROCESSING,
                    0,
                    fileMinifyTimeout,
                    (message) -> {
                        log.info("Received {} to process : {}", ActionType.PROCESS_FILE.name(), message.getFileId());
                        processFileService.process(message.getFileId());
                    },
                    (jobContext) -> {
                        log.info("Ending processing");
                        logAggregator.sendWorkerLogs(
                                jobContext.getProcessId(),
                                ActionType.PROCESS_FILE.name(),
                                jobContext.getStartTime(),
                                JobContextUtil.prepareJobAttributes(jobContext)
                        );
                    });
        } catch (Exception e) {
            log.error("Unable to consume the message queue");
        }
    }
}
