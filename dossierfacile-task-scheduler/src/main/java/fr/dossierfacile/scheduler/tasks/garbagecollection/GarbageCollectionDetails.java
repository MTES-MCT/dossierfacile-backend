package fr.dossierfacile.scheduler.tasks.garbagecollection;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "garbage_collection")
public class GarbageCollectionDetails {

    @Id
    @Enumerated(EnumType.STRING)
    ObjectStorageProvider provider;

    @Column
    String currentMarker;

    @Column
    int deletedObjects;

}
