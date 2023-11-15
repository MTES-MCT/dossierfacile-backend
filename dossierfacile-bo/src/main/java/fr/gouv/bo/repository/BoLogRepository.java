package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoLogRepository extends JpaRepository<Log, Long> {

    List<Log> findLogsByTenantId(Long id);

    Page<Log> findAll(Pageable pageable);

    Page<Log> findAllByTenantId(Long tenantId, Pageable pageable);

    @Query(value = """
            SELECT DATE(creation_date) AS creation_date, COUNT(*) AS record_count
            FROM tenant_log
            WHERE operator_id = :operatorId
                AND ( log_type = 'ACCOUNT_VALIDATED' OR log_type = 'ACCOUNT_DENIED' )
                AND creation_date BETWEEN CURRENT_DATE - :minusDays AND CURRENT_DATE + 1
            GROUP BY DATE(creation_date)
            """, nativeQuery = true)
    List<Object[]> countTreatedFromXDaysGroupByDate(@Param("operatorId") Long operatorId, @Param("minusDays") int minusDays);
}
