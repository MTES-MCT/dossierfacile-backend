package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Log;
import fr.gouv.bo.repository.BoLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class LogService {

    private final BoLogRepository logRepository;

    public List<Log> getLogByTenantId(Long tenantId) {
        List<Log> logList = logRepository.findLogsByTenantId(tenantId);
        logList.sort(Comparator.comparing(Log::getCreationDateTime).reversed());
        return logList;
    }

    public Page<Log> findAllPageable(PageRequest page) {
        return logRepository.findAll(page);
    }

    public Page<Log> findAllByTenantIdPageable(Long tenantId, PageRequest page) {
        return logRepository.findAllByTenantId(tenantId, page);
    }

    public void saveByLog(Log log) {
        logRepository.save(log);
    }

    public List<Object[]> listLastTreatedFilesByOperator(Long operatorId, int minusDays){
        return logRepository.countTreatedFromXDaysGroupByDate(operatorId, minusDays);
    }

    public List<Object[]> listDailyTreatedFilesByOperator() {
        return logRepository.countTreatedFromTodayGroupByOperator();
    }
}
