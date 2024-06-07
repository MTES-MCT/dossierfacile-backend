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
public class QueueMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long timestamp;
    private Long documentId;
    @Enumerated(EnumType.STRING)
    private QueueMessageStatus status;
}
