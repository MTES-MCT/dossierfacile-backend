package fr.dossierfacile.api.front.repository;

import fr.dossierfacile.common.entity.LinkLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkLogRepository extends JpaRepository<LinkLog, Long> {
}
