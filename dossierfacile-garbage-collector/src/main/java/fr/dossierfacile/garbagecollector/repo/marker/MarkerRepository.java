package fr.dossierfacile.garbagecollector.repo.marker;

import fr.dossierfacile.garbagecollector.model.marker.Marker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkerRepository extends JpaRepository<Marker, Long> {
}
