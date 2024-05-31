package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.TenantLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantLogRepository extends JpaRepository<TenantLog, Long> {

    @Query(value = "select l.*\n" +
            "from tenant_log l\n" +
            "  join tenant t on t.id = l.tenant_id\n" +
            "  join apartment_sharing app on t.apartment_sharing_id = app.id\n" +
            "where t.apartment_sharing_id = :apartId\n" +
            " and l.log_type='ACCOUNT_VALIDATED' order by creation_date desc limit 1 \n", nativeQuery = true)
    Optional<TenantLog> findLastValidationLogByApartmentSharing(@Param("apartId") Long apartmentSharingId);

}
