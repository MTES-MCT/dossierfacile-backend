package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.messaging.QueueMessage;
import fr.dossierfacile.common.entity.messaging.QueueMessageStatus;
import fr.dossierfacile.common.entity.messaging.QueueName;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface QueueMessageRepository extends JpaRepository<QueueMessage, Long> {
    List<QueueMessage> findByQueueNameAndDocumentIdAndStatusIn(QueueName queueName, Long documentId, List<QueueMessageStatus> queueMessageStatus);

    Optional<QueueMessage> findFirstByQueueNameAndDocumentIdAndStatus(QueueName queueName, Long documentId, QueueMessageStatus status);

    Optional<QueueMessage> findFirstByQueueNameAndFileIdAndStatus(QueueName queueName, Long fileId, QueueMessageStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    QueueMessage findFirstByStatusAndQueueNameAndTimestampLessThanOrderByTimestampAsc(
            QueueMessageStatus status,
            QueueName queueName,
            long toTimestamp);

    /**
     * Dépile le premier message PENDING disponible.
     * 'FOR UPDATE SKIP LOCKED' évite la contention entre workers concurrents en sautant les lignes
     * déjà verrouillées par un autre worker plutôt que de bloquer en attente SQL.
     */
    @Query(value = """
            SELECT * FROM queue_message
            WHERE queue_name = :queueName
              AND status = :status
              AND timestamp < :toTimestamp
            ORDER BY timestamp ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    QueueMessage findFirstByStatusAndQueueNameAndTimestampLessThanOrderByTimestampAscSkipLocked(
            @Param("status") String status,
            @Param("queueName") String queueName,
            @Param("toTimestamp") long toTimestamp);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM queue_message
            WHERE id IN (
                SELECT id
                FROM (
                    SELECT id, ROW_NUMBER() OVER (PARTITION BY queue_name, document_id, file_id, status ORDER BY timestamp DESC) AS rn
                    FROM queue_message
                    WHERE status = 'PENDING'
                    AND queue_name = :queueName
                ) qm
                WHERE qm.rn > 1
            );
            """, nativeQuery = true)
    void cleanQueue(@Param("queueName") String queueName);
}
