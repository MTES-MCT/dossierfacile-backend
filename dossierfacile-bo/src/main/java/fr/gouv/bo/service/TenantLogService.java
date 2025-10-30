package fr.gouv.bo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.model.log.EditedDocument;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.model.log.UpdateMonthlySum;
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
                .logDetails(writeAsObjectNode(Map.of("comment", operatorComment)))
                .build();
        saveByLog(log);
    }

    public void addDeleteDocumentLog(Long tenantId, Long operatorId, Document document) {
        TenantLog log = TenantLog.builder()
            .logType(LogType.ACCOUNT_EDITED)
            .tenantId(tenantId)
            .operatorId(operatorId)
            .creationDateTime(LocalDateTime.now())
            .logDetails(writeAsObjectNode(EditedDocument.from(document, EditionType.DELETE)))
            .build();
        saveByLog(log);
    }

    public void addUpdateAmountLog(Long tenantId, Long operatorId, Document document, Integer newSum) {
        TenantLog log = TenantLog.builder()
            .logType(LogType.ACCOUNT_EDITED)
            .tenantId(tenantId)
            .operatorId(operatorId)
            .creationDateTime(LocalDateTime.now())
            .logDetails(writeAsObjectNode(UpdateMonthlySum.from(document, newSum)))
            .build();
        saveByLog(log);
    }

    public List<Object[]> listLastTreatedFilesByOperator(Long operatorId, int minusDays) {
        return logRepository.countTreatedFromXDaysGroupByDate(operatorId, minusDays);
    }

    public List<Object[]> listDailyTreatedFilesByOperator() {
        return logRepository.countTreatedFromTodayGroupByOperator();
    }

    public List<TenantLog> getDocumentLogs(List<TenantLog> logs, Long documentId) {
        return logs.stream()
            .filter(l -> documentId.equals(l.getLogDetails().get("documentId").asLong()))
            .toList();
    }

    private ObjectNode writeAsObjectNode(Object object) {
        try {
            return (ObjectNode) objectMapper.valueToTree(object);
        } catch (IllegalArgumentException e) {
            log.error("FATAL: Cannot write log details as object node", e);
        }
        return null;
    }


}
