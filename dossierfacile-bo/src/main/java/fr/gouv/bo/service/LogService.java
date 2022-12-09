package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Log;
import fr.gouv.bo.repository.LogRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public Page<Log> findAllPageable(PageRequest page) {
        return logRepository.findAll(page);
    }
    public Page<Log> findAllByTenantIdPageable(Long tenantId, PageRequest page) {
        return logRepository.findAllByTenantId(tenantId, page);
    }
}
