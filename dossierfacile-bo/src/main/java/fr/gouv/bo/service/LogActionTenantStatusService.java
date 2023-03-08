package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Log;
import fr.gouv.bo.repository.BoLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LogActionTenantStatusService {
    private final BoLogRepository logRepository;

    public void saveByLog(Log log){
        logRepository.save(log);
    }
}
