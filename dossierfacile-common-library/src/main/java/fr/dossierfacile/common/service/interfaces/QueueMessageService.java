package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueName;

import java.util.function.Consumer;

public interface QueueMessageService {
    void consume(QueueName queueName, long consumptionDelayInMillis, long consumptionTimeout, Consumer<QueueMessage> messageConsumer);
}
