package fr.dossierfacile.process.file.amqp;

import fr.dossierfacile.process.file.service.AnalyzeFile;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
@Setter // for testing
public class AnalyzeFileReceiver {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final AnalyzeFile analyzeFile;
    @Value("${analysis.timeout.seconds}")
    private Long analysisTimeoutInSeconds;

    @RabbitListener(queues = "${rabbitmq.queue.file.analyze}", containerFactory = "retryContainerFactory")
    public void receiveFile(Map<String, String> message) throws InterruptedException, ExecutionException {
        Long fileId = Long.valueOf(message.get("id"));
        LoggingContext.startProcessing(fileId, ActionType.ANALYZE);
        var analysis = launchAnalysis(fileId);
        try {
            analysis.get(analysisTimeoutInSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Try to cancel");
            analysis.cancel(true);
            log.warn("Analysis cancelled because timeout was reached");
        } catch (Exception e) {
            log.error("Failed to analyze file", e);
            throw e;
        } finally {
            LoggingContext.endProcessing();
        }
    }

    private Future<?> launchAnalysis(Long fileId) {
        Runnable task = () -> analyzeFile.processFile(fileId);
        return submitTask(task);
    }

    private Future<?> submitTask(Runnable runnable) {
        return executor.submit(LoggingContext.copyContextInThread(runnable));
    }

}
