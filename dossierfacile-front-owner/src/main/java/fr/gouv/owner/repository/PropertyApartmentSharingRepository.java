package fr.gouv.owner.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PropertyApartmentSharingRepository extends CrudRepository<PropertyApartmentSharing, Long> {
    List<PropertyApartmentSharing> findAllByPropertyId(Long id);

}
