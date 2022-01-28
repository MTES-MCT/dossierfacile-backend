package fr.dossierfacile.api.dossierfacileapiowner.log;

import fr.dossierfacile.common.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {

}
