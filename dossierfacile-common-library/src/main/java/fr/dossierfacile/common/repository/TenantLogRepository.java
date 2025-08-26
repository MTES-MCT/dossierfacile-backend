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
                SELECT id, tenant_id
                FROM tenant_log
                WHERE id > :startId
                ORDER BY id
                LIMIT :windowSize
            ), tenants AS (
                SELECT DISTINCT tenant_id
                FROM logs_window
            )
            SELECT t.tenant_id AS tenantId,
                   x.log_type AS status,
                   x.creation_date AS creationDate
            FROM tenants t
            CROSS JOIN LATERAL (
                SELECT log_type, creation_date, id
                FROM tenant_log l
                WHERE l.tenant_id = t.tenant_id
                ORDER BY creation_date DESC, id DESC
                LIMIT 1
            ) x
            ORDER BY x.creation_date DESC, x.id DESC
            """, nativeQuery = true)
    List<TenantLastStatusProjection> findLastStatusBatchAfterLogId(@Param("startId") long startId,
                                                                  @Param("windowSize") int windowSize);

    default List<TenantLastStatus> findLastStatusBatch(long startId, int windowSize) {
        return findLastStatusBatchAfterLogId(startId, windowSize).stream()
                .map(p -> new TenantLastStatus(p.getTenantId(), LogType.valueOf(p.getStatus()), p.getCreationDate()))
                .toList();
    }
}
