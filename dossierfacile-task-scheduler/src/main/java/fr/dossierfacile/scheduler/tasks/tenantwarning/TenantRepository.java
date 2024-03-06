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
    Page<Tenant> findByLastLoginDateIsBeforeAndHasDocuments(Pageable pageable, @Param("localDateTime") LocalDateTime localDateTime, @Param("warnings") Integer warnings);

    @Query(value = """
           SELECT *
           FROM tenant t, user_account u
           WHERE t.id = u.id
             AND t.tenant_type = 'JOIN'
             AND t.status != 'ARCHIVED'
             AND u.email is null
             AND EXISTS (
               SELECT 1 FROM tenant t2
               WHERE t2.apartment_sharing_id = t.apartment_sharing_id
                 AND t2.tenant_type = 'CREATE'
                 AND t2.status = 'ARCHIVED'
           )
           """, nativeQuery = true)
    Page<Tenant> findCotenantsWithNoEmailAndArchivedMainTenant(Pageable pageable);

}
