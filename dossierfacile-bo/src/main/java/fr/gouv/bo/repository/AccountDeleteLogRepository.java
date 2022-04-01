package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.AccountDeleteLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountDeleteLogRepository extends JpaRepository<AccountDeleteLog, Long> {
}
