package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.OperatorLog;
import fr.dossierfacile.common.enums.ActionOperatorType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface OperatorLogRepository extends JpaRepository<OperatorLog, Long> {
    Long countByOperatorIdAndActionOperatorTypeAndCreationDateGreaterThanEqual(Long operatorId, ActionOperatorType actionType, LocalDateTime startDate);
}
