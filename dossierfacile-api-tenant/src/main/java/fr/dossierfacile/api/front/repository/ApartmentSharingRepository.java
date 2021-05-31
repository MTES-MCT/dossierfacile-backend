package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApartmentSharingRepository extends JpaRepository<ApartmentSharing, Long> {
    @Query(value = "select a.* from apartment_sharing a join tenant t on a.id = t.apartment_sharing_id where t.id=:id", nativeQuery = true)
    Optional<ApartmentSharing> findByTenant(@Param("id") Long id);

    Optional<ApartmentSharing> findByToken(String token);

    Optional<ApartmentSharing> findByTokenPublic(String token);
}
