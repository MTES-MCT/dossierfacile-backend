package fr.dossierfacile.api.dossierfacileapiowner.user;

import fr.dossierfacile.common.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
    Optional<Owner> findByEmailAndEnabledFalse(String email);

    Optional<Owner> findByEmail(String email);

    Optional<Owner> findByKeycloakId(String keycloakId);

    Owner findOneByEmailAndEnabledTrue(String email);

    @Query("select distinct o FROM Owner o " +
            " join o.properties p " +
            " join p.propertiesApartmentSharing pas "+
            " join pas.apartmentSharing ass  " +
            " join ass.tenants t  " +
            "where t.id in (:tenantIds)")
    List<Owner> findAllByTenantIds(List<Long> tenantIds);
}

