package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.AccountDeleteLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountDeleteLogCommonRepository extends JpaRepository<AccountDeleteLog, Long> {
    List<AccountDeleteLog> findByUserId(Long userId);
}
