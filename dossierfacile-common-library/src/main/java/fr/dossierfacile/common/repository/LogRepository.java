package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {

}
