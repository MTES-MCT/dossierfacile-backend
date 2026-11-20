package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Batch queries use keyset pagination (id strictly lower than a cursor, ordered by id desc)
 * instead of offset pagination: processed tenants leave the result set (warned, archived or deleted),
 * so an offset would skip tenants that were never processed.
 * The {@link Pageable} parameter is only used as a limit and must target page 0.
 */
@Repository
interface TenantRepository extends JpaRepository<Tenant, Long> {

    @Query("""
            select t.id from Tenant t
            where t.lastUpdateDate < :before
            and t.id < :beforeId
            order by t.id desc
            """)
    List<Long> findIdsByLastUpdateDateBefore(@Param("before") LocalDateTime before,
            @Param("beforeId") Long beforeId,
            Pageable limit);

    @Query("select count(t) from Tenant t where t.lastUpdateDate < :before")
    long countByLastUpdateDateBefore(@Param("before") LocalDateTime before);

    @Query("""
            select t from Tenant t
            where t.lastLoginDate < :localDateTime
            and t.warnings = :warnings
            and t.id < :beforeId
            and exists (
                select 1 from Document d where d.tenant.id = t.id
            )
            order by t.id desc
            """)
    List<Tenant> findInactiveTenantsWithDocuments(@Param("localDateTime") LocalDateTime localDateTime,
            @Param("warnings") Integer warnings,
            @Param("beforeId") Long beforeId,
            Pageable limit);

    @Query("""
            select count(t) from Tenant t
            where t.lastLoginDate < :localDateTime
            and t.warnings = :warnings
            and exists (
                select 1 from Document d where d.tenant.id = t.id
            )
            """)
    long countInactiveTenantsWithDocuments(@Param("localDateTime") LocalDateTime localDateTime,
            @Param("warnings") Integer warnings);

    @Query("""
            select t from Tenant t
            where t.lastLoginDate < :localDateTime
            and t.status != 'ARCHIVED'
            and t.id < :beforeId
            and not exists (
                        select 1 from Document d where d.tenant.id = t.id
            )
            order by t.id desc
            """)
    List<Tenant> findInactiveTenantsWithoutDocuments(@Param("localDateTime") LocalDateTime localDateTime,
            @Param("beforeId") Long beforeId,
            Pageable limit);

    @Query("""
            select count(t) from Tenant t
            where t.lastLoginDate < :localDateTime
            and t.status != 'ARCHIVED'
            and not exists (
                        select 1 from Document d where d.tenant.id = t.id
            )
            """)
    long countInactiveTenantsWithoutDocuments(@Param("localDateTime") LocalDateTime localDateTime);

}
