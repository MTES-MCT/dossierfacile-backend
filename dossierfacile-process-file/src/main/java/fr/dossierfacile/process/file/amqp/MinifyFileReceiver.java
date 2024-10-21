package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.service.interfaces.QueueMessageService;
import fr.dossierfacile.process.file.service.interfaces.MinifyFileService;
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
public class MinifyFileReceiver {

    private final MinifyFileService minifyFileService;
    private final QueueMessageService queueMessageService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${file.minify.timeout.ms}")
    private Long fileMinifyTimeout;

    @PostConstruct
    public void startConsumer() {
        scheduler.scheduleAtFixedRate(this::receiveFile, 0, 2, TimeUnit.SECONDS);
    }

    private void receiveFile() {
        try {
            queueMessageService.consume(QueueName.QUEUE_FILE_MINIFY,
                    0,
                    fileMinifyTimeout,
                    (message) -> {
                        LoggingContext.startProcessing(message.getFileId(), ActionType.MINIFY);
                        minifyFileService.process(message.getFileId());
                        LoggingContext.endProcessing();
                    });
        } catch (Exception e) {
            log.error("Unable to consume the message queue");
        }
    }
}
