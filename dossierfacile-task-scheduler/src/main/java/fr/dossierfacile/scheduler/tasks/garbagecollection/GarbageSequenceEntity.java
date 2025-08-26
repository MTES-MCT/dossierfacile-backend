package fr.dossierfacile.scheduler.tasks.garbagecollection;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * This entity is used to keep track of the last value of a sequence used for garbage collection.
 * It is used to avoid scanning the same objects multiple times.
 */

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "garbage_sequence")
public class GarbageSequenceEntity {

    @Id
    @Enumerated(EnumType.STRING)
    GarbageSequenceName name;

    @Column
    int value;

    @Column
    LocalDateTime lastUpdateDate;

}
