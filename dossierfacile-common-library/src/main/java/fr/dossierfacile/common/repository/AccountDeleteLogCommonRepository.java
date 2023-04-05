package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.AccountDeleteLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountDeleteLogCommonRepository extends JpaRepository<AccountDeleteLog, Long> {
}
