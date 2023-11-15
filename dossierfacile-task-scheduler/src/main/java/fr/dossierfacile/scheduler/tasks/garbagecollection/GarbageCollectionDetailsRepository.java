package fr.dossierfacile.scheduler.tasks.garbagecollection;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarbageCollectionDetailsRepository extends JpaRepository<GarbageCollectionDetails, ObjectStorageProvider> {

}
