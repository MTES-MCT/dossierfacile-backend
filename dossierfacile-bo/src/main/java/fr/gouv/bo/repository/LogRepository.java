package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogRepository extends JpaRepository<Log, Long> {

    List<Log> findLogsByTenantId(Long id);

}
