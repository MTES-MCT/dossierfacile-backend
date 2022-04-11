package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.PropertyApartmentSharing;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PropertyApartmentSharingRepository extends CrudRepository<PropertyApartmentSharing, Long> {
    List<PropertyApartmentSharing> findPropertyApartmentSharingsByApartmentSharingId(Long id);

}
