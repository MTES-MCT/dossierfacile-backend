package fr.dossierfacile.garbagecollector.repo.marker;

import fr.dossierfacile.garbagecollector.model.marker.Marker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MarkerRepository extends JpaRepository<Marker, Long> {
    Marker findByPath(String path);

    @Query(value = "SELECT * FROM marker ORDER BY id DESC LIMIT 2", nativeQuery = true)
    List<Marker> lastTwoMarkers();
}
