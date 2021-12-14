package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.CallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallbackLogRepository extends JpaRepository<CallbackLog, Long> {

}
