package fr.dossierfacile.garbagecollector.model.garbagecollection;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

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
