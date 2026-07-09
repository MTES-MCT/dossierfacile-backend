package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.OperatorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatorLogCommonRepository extends JpaRepository<OperatorLog, Long> {
}
