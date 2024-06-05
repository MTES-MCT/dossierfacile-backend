package fr.gouv.bo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.gouv.bo.repository.BoTenantLogRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class TenantLogService {

    private final BoTenantLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public List<TenantLog> getLogByTenantId(Long tenantId) {
        List<TenantLog> logList = logRepository.findLogsByTenantId(tenantId);
        logList.sort(Comparator.comparing(TenantLog::getCreationDateTime).reversed());
        return logList;
    }

    public Page<TenantLog> findAllPageable(PageRequest page) {
        return logRepository.findAll(page);
    }

    public Page<TenantLog> findAllByTenantIdPageable(Long tenantId, PageRequest page) {
        return logRepository.findAllByTenantId(tenantId, page);
    }

    public void saveByLog(TenantLog log) {
        logRepository.save(log);
    }

    public void addOperatorCommentLog(Tenant tenant, String operatorComment) {
        TenantLog log = TenantLog.builder()
                .logType(LogType.OPERATOR_COMMENT)
                .tenantId(tenant.getId())
                .creationDateTime(LocalDateTime.now())
                .logDetails(writeAsString(Map.of("comment", operatorComment)))
                .build();
        saveByLog(log);
    }

    public List<Object[]> listLastTreatedFilesByOperator(Long operatorId, int minusDays) {
        return logRepository.countTreatedFromXDaysGroupByDate(operatorId, minusDays);
    }

    public List<Object[]> listDailyTreatedFilesByOperator() {
        return logRepository.countTreatedFromTodayGroupByOperator();
    }

    private String writeAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("FATAL: Cannot write log details as string", e);
        }
        return null;
    }
}
