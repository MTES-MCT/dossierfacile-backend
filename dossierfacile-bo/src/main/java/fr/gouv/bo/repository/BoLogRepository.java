package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoLogRepository extends JpaRepository<Log, Long> {

    List<Log> findLogsByTenantId(Long id);

    Page<Log> findAll(Pageable pageable);

    Page<Log> findAllByTenantId(Long tenantId, Pageable pageable);
}
