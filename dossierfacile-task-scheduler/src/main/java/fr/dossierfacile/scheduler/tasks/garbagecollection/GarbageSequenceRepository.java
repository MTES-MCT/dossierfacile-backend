package fr.dossierfacile.scheduler.tasks.garbagecollection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarbageSequenceRepository extends JpaRepository<GarbageSequenceEntity, GarbageSequenceName> {
    GarbageSequenceEntity getGarbageSequenceByName(GarbageSequenceName name);
}
