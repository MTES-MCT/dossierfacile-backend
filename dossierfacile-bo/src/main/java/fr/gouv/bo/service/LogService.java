package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Log;
import fr.gouv.bo.repository.LogRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    public List<Log> getLogById(Long id) {
        List<Log> logList = logRepository.findLogsByTenantId(id);
        logList.sort(Comparator.comparing(Log::getCreationDateTime).reversed());
        return logList;
    }
}
