package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantLogRepository extends JpaRepository<Log, Long> {

    @Query(value = "select l.*\n" +
            "from tenant_log l\n" +
            "  join tenant t on t.id = l.tenant_id\n" +
            "  join apartment_sharing app on t.appartment_sharing_id = app.id\n" +
            "where t.apartment_sharing_id = :apartId\n" +
            " and l.log_type='ACCOUNT_VALIDATED' order by creation_date desc \n", nativeQuery = true)
    Optional<Log> findLastValidationLogByApartmentSharing(@Param("apartId") Long apartmentSharingId);

}
