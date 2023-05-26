package fr.dossierfacile.api.dossierfacileapiowner.property;

import fr.dossierfacile.common.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findAllByOwnerId(Long ownerId);

    Optional<Property> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<Property> findByToken(String token);

    @Query("select distinct p FROM Property p " +
            " join p.propertiesApartmentSharing pas "+
            " join pas.apartmentSharing ass  " +
            " join ass.tenants t  " +
            "where t.id in (:tenantIds)")
    List<Property> findAllByTenantIds(List<Long> tenantIds);
}
