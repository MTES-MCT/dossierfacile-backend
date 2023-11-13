package fr.dossierfacile.scheduler.tasks.tenantwarning;

import fr.dossierfacile.common.entity.Tenant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
interface TenantRepository extends JpaRepository<Tenant, Long> {

    @Query("SELECT t.id FROM Tenant t WHERE t.lastUpdateDate < :before")
    List<Long> findByLastUpdateDate(@Param("before") LocalDateTime before, Pageable pageable);

}
