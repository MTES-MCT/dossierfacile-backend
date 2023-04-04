package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.process.file.service.AnalyzeFile;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class AnalyzeFileReceiver {

    private static final String EXCEPTION = "Sentry ID Exception: ";

    private final AnalyzeFile analyzeFile;

    @RabbitListener(queues = "${rabbitmq.queue.file.analyze}", containerFactory = "retryContainerFactory")
    public void processDocument(Map<String, String> message) {
        try {
            Long fileId = Long.valueOf(message.get("id"));
            log.info("Received file [{}] to analyze", fileId);
            analyzeFile.processFile(fileId);
        } catch (Exception e) {
            log.error(EXCEPTION + Sentry.captureException(e), e);
            throw e;
        }
    }

}
