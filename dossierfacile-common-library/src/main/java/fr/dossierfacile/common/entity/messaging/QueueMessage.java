package fr.dossierfacile.common.entity.messaging;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "queue_message")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class QueueMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private QueueName queueName;
    private Long timestamp;
    private Long documentId;
    private Long fileId;
    @Enumerated(EnumType.STRING)
    private QueueMessageStatus status;
}
