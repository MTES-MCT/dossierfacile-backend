package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.process.file.service.interfaces.MinifyFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class MinifyFileReceiver {

    private final MinifyFile minifyFile;

    @RabbitListener(queues = "${rabbitmq.queue.file.minify}", containerFactory = "retryContainerFactory")
    public void processFileTax(Map<String, String> item) {
        Long fileId = Long.valueOf(item.get("id"));
        LoggingContext.startProcessing(fileId, ActionType.MINIFY);
        try {
            minifyFile.process(fileId);

        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            throw e;
        } finally {
            LoggingContext.endProcessing();
        }
    }
}
