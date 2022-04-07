package fr.dossierfacile.garbagecollector.repo.apartment;

import fr.dossierfacile.garbagecollector.model.apartment.ApartmentSharing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface apartmentRepo extends JpaRepository<ApartmentSharing, Long> {

}
