package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QueueMessageRepository extends JpaRepository<QueueMessage, Long> {
    QueueMessage findByQueueNameAndDocumentIdAndStatus(QueueName queueName, Long documentId, QueueMessageStatus queueMessageStatus);

    @Query(value = """
            SELECT * FROM queue_message
            WHERE status = 'PENDING'
            AND queue_name = :queueName
            AND timestamp < :toTimestamp
            ORDER BY timestamp ASC
            LIMIT 1
            """, nativeQuery = true)
    QueueMessage findFirstMessage(@Param("queueName") String queueName, @Param("toTimestamp") long toTimestamp);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM queue_message
            WHERE id IN (
                SELECT id
                FROM (
                    SELECT id, ROW_NUMBER() OVER (PARTITION BY queue_name, document_id, status ORDER BY timestamp DESC) AS rn
                    FROM queue_message
                    WHERE status = 'PENDING'
                    AND queue_name = :queueName
                ) qm
                WHERE qm.rn > 1
            );
            """, nativeQuery = true)
    void cleanQueue(@Param("queueName") String queueName);
}
