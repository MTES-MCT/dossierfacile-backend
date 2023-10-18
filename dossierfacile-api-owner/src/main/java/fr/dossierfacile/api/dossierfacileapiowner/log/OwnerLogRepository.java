package fr.dossierfacile.api.dossierfacileapiowner.log;

import fr.dossierfacile.common.entity.OwnerLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerLogRepository extends JpaRepository<OwnerLog, Long> {
}