package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Log;
import fr.gouv.bo.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LogActionTenantStatusService {
    private final LogRepository logRepository;

    public void saveByLog(Log log){
        logRepository.save(log);
    }
}
