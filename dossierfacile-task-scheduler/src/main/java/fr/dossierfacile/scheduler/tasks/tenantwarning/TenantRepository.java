package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
interface TenantRepository extends JpaRepository<Tenant, Long> {

    @Query("SELECT t.id FROM Tenant t WHERE t.lastUpdateDate < :before")
    Page<Long> findByLastUpdateDate(@Param("before") LocalDateTime before, Pageable pageable);

    @Query(value = """
            select t from Tenant t
            where t.lastLoginDate < :localDateTime
            and t.warnings = :warnings
            and t.id in (select d.tenant.id from Document d where d.tenant.id is not null)
            """)
    Page<Tenant> findInactiveTenantsWithDocuments(Pageable pageable, @Param("localDateTime") LocalDateTime localDateTime, @Param("warnings") Integer warnings);

}
