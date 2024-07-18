package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.common.service.interfaces.QueueMessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
@Slf4j
@AllArgsConstructor
public class QueueMessageServiceImpl implements QueueMessageService {

    private final QueueMessageRepository queueMessageRepository;

    @Override
    public void consume(QueueName queueName, long consumptionDelayInMillis, long consumptionTimeout, Consumer<QueueMessage> consumer) {
        queueMessageRepository.cleanQueue(queueName.name());
        long toTimestamp = System.currentTimeMillis() - consumptionDelayInMillis;
        QueueMessage message = queueMessageRepository.popFirstMessage(queueName.name(), toTimestamp);
        if (message != null) {
            log.info("Received message on {} to process: {}", queueName, message);
            try {
                this.consumeMessageWithTimeout(consumer, consumptionTimeout, message);
                queueMessageRepository.delete(message);
            } catch (InterruptedException e) {
                message.setStatus(QueueMessageStatus.FAILED);
                queueMessageRepository.save(message);
                log.error("Thread interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                message.setStatus(QueueMessageStatus.FAILED);
                queueMessageRepository.save(message);
            }
        }
    }

    private void consumeMessageWithTimeout(Consumer<QueueMessage> consumer, long consumptionTimeout, QueueMessage message) throws TimeoutException, ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            consumer.accept(message);
        });
        try {
            future.get(consumptionTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Timeout while consume message {}", message.getDocumentId());
            throw e;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while consume message {}", message.getDocumentId(), e);
            throw e;
        }
    }
}
