package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.UserApi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApartmentSharingRepository extends JpaRepository<ApartmentSharing, Long> {
    @Query(value = "select a.* from apartment_sharing a join tenant t on a.id = t.apartment_sharing_id where t.id=:id", nativeQuery = true)
    Optional<ApartmentSharing> findByTenant(@Param("id") Long id);

    Optional<ApartmentSharing> findByToken(String token);

    Optional<ApartmentSharing> findByTokenPublic(String token);

    @Query(value = """
            SELECT a 
            FROM ApartmentSharing a
            JOIN a.tenants t
            JOIN t.tenantsUserApi tua
            WHERE tua.userApi =:partner
            AND ( cast(cast(:lastUpdateDate as text) as timestamp) is null or a.lastUpdateDate > cast(cast(:lastUpdateDate as text) as timestamp))
            ORDER BY t.lastUpdateDate ASC
            """
    )
    List<ApartmentSharing> findByLastUpdateDateAndPartner(@Param("lastUpdateDate") LocalDateTime lastUpdateDateFrom, @Param("partner") UserApi partner, Pageable pageable);

}
