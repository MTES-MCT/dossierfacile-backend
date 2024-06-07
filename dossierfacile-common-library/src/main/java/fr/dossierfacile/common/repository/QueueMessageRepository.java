package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueueMessageRepository extends JpaRepository<QueueMessage, Long> {
    QueueMessage findByDocumentIdAndStatus(Long documentId, QueueMessageStatus queueMessageStatus);

    @Query(value = """
            SELECT * FROM queue_message
            WHERE status = 'PENDING'
            ORDER BY timestamp ASC
            LIMIT 1
            """, nativeQuery = true)
    QueueMessage pop();
}
