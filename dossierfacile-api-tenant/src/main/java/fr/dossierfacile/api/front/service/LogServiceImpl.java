package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.repository.LogRepository;
import fr.dossierfacile.api.front.service.interfaces.LogService;
import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.enums.LogType;
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
