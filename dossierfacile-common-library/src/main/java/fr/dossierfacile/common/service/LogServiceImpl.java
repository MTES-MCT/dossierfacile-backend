package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.repository.LogRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogRepository repository;

    @Override
    public void saveLog(LogType logType, Long tenantId) {
        repository.save(new Log(logType,tenantId));
    }

}
