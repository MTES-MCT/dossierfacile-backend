package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.TenantLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface TenantLogRepository extends JpaRepository<TenantLog, Long> {

    @Query(value = """
            SELECT COUNT(*) AS record_count
            FROM tenant_log t
            WHERE ( t.log_type = 'ACCOUNT_VALIDATED' OR log_type = 'ACCOUNT_DENIED' )
                AND t.creation_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 1
            """, nativeQuery = true)
    long countProcessedDossiersFromToday();
}
