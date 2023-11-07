package fr.dossierfacile.scheduler.repo.garbagecollection;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.scheduler.model.garbagecollection.GarbageCollectionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarbageCollectionDetailsRepository extends JpaRepository<GarbageCollectionDetails, ObjectStorageProvider> {

}
