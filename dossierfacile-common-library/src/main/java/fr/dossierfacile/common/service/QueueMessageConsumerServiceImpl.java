package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import fr.dossierfacile.common.repository.QueueMessageRepository;
import fr.dossierfacile.common.service.interfaces.QueueMessageConsumerService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class QueueMessageConsumerServiceImpl implements QueueMessageConsumerService {
    private final QueueMessageRepository queueMessageRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public QueueMessage popFirstMessage(QueueName queueName, long toTimestamp) {
        QueueMessage msg = queueMessageRepository.findFirstByStatusAndQueueNameAndTimestampLessThanOrderByTimestampAsc(
                QueueMessageStatus.PENDING, queueName, toTimestamp);
        if (msg != null) {
            msg.setStatus(QueueMessageStatus.PROCESSING);
            queueMessageRepository.save(msg);
        }
        return msg;
    }
}