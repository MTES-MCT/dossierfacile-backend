package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueName;

public interface QueueMessageConsumerService {
    QueueMessage popFirstMessage(QueueName queueName, long toTimestamp);
}
