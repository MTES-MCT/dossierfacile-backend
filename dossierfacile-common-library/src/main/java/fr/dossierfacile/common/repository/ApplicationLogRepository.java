package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {
    Optional<ApplicationLog> findFirstByLogTypeAndApiVersion(ApplicationLog.ApplicationLogType applicationLogType, int i);
}
