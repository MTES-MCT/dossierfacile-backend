package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.TenantLog;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoTenantLogRepository extends JpaRepository<TenantLog, Long> {

    List<TenantLog> findLogsByTenantId(Long id);

    Page<TenantLog> findAll(Pageable pageable);

    Page<TenantLog> findAllByTenantId(Long tenantId, Pageable pageable);

    @Cacheable("treated-files-by-operator-key")
    @Query(value = """
            SELECT DATE(creation_date) AS creation_date, COUNT(*) AS record_count
            FROM tenant_log
            WHERE operator_id = :operatorId
                AND ( log_type = 'ACCOUNT_VALIDATED' OR log_type = 'ACCOUNT_DENIED' )
                AND creation_date BETWEEN CURRENT_DATE - :minusDays AND CURRENT_DATE + 1
            GROUP BY DATE(creation_date)
            """, nativeQuery = true)
    List<Object[]> countTreatedFromXDaysGroupByDate(@Param("operatorId") Long operatorId, @Param("minusDays") int minusDays);

    @Query(value = """
            SELECT ua.email AS operator, COUNT(*) AS record_count
            FROM tenant_log t
            INNER JOIN user_account ua ON t.operator_id = ua.id\s
            WHERE ( t.log_type = 'ACCOUNT_VALIDATED' OR log_type = 'ACCOUNT_DENIED' )
                AND t.creation_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 1
            GROUP BY ua.email
            """, nativeQuery = true)
    List<Object[]> countTreatedFromTodayGroupByOperator();
}
