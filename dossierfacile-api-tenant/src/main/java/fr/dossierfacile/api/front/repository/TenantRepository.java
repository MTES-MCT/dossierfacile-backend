package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByEmailAndEnabledFalse(String email);

    @Query("from Tenant t where t.email in :emails")
    List<Tenant> findByListEmail(@Param("emails") List<String> emails);

    List<Tenant> findAllByApartmentSharing(ApartmentSharing apartmentSharing);

    Optional<Tenant> findByEmail(String name);
}
