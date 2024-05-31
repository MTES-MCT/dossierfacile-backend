package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.TenantLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantLogRepository extends JpaRepository<TenantLog, Long> {

}
