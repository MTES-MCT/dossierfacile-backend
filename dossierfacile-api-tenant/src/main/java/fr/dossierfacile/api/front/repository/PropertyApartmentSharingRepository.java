package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Property;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyApartmentSharingRepository extends JpaRepository<PropertyApartmentSharing, Long> {
    Optional<PropertyApartmentSharing> findByPropertyAndApartmentSharing(Property property, ApartmentSharing apartmentSharing);
}