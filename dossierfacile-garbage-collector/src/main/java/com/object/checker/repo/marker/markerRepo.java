package com.object.checker.repo.marker;

import com.object.checker.model.marker.Marker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface markerRepo extends JpaRepository<Marker, Long> {

    Marker findByPath(String text);
}
