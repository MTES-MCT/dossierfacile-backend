package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.TenantLastStatus;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.repository.projection.TenantLastStatusProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TenantLogRepository extends JpaRepository<TenantLog, Long> {

    @Query(value = """
            SELECT COUNT(*) AS record_count
            FROM tenant_log t
            WHERE ( t.log_type = 'ACCOUNT_VALIDATED' OR log_type = 'ACCOUNT_DENIED' )
                AND t.creation_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 1
            """, nativeQuery = true)
    long countProcessedDossiersFromToday();

    @Query(value = """
            WITH logs_window AS (
                SELECT id, tenant_id, creation_date, log_type
                FROM tenant_log
                WHERE id > :startId
                ORDER BY id desc
            )
            SELECT DISTINCT
                tenant_id AS tenantId,
                LAST_VALUE(log_type) over (
                    partition by tenant_id
                    order by creation_date asc
                    rows between unbounded preceding and unbounded following
                ) as lastStatus,
                LAST_VALUE(creation_date) over (
                    partition by tenant_id
                    order by creation_date asc
                    rows between unbounded preceding and unbounded following
                    ) as lastStatusAt,
                (select id from tenant_log order by id desc limit 1) as lastLogId
            FROM logs_window
            """, nativeQuery = true)
    List<TenantLastStatusProjection> findLastStatusBatchAfterLogId(@Param("startId") long startId);

    default List<TenantLastStatus> findLastStatusBatch(long startId) {
        return findLastStatusBatchAfterLogId(startId).stream()
                .map(p -> new TenantLastStatus(p.getTenantId(), LogType.valueOf(p.getLastStatus()), p.getLastStatusAt(), p.getLastLogId()))
                .toList();
    }
}
