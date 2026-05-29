package fr.gouv.bo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.model.log.DocumentLogDetails;
import fr.dossierfacile.common.model.log.FileLogDetails;
import fr.dossierfacile.common.model.log.UpdateMonthlySum;
import fr.dossierfacile.common.service.interfaces.TenantLogCommonService;
import fr.gouv.bo.repository.BoTenantLogRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final TenantLogCommonService tenantLogCommonService;
    private final ObjectMapper objectMapper;

    public List<TenantLog> getLogByTenantId(Long tenantId) {
        List<TenantLog> logList = logRepository.findLogsByTenantId(tenantId);
        logList.sort(Comparator.comparing(TenantLog::getCreationDateTime).reversed());
        return logList;
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

    public void addOperatorCommentLog(Long operatorId, Long tenantId, String operatorComment) {
        TenantLog log = TenantLog.builder()
                .logType(LogType.OPERATOR_COMMENT)
                .tenantId(tenantId)
                .operatorId(operatorId)
                .creationDateTime(LocalDateTime.now())
                .logDetails(writeAsObjectNode(Map.of("comment", operatorComment)))
                .build();
        tenantLogCommonService.saveTenantLog(log);
    }

    public void addDeleteDocumentLog(Long tenantId, Long operatorId, Document document) {
        TenantLog log = TenantLog.builder()
            .logType(LogType.DOCUMENT_DELETED)
            .tenantId(tenantId)
            .operatorId(operatorId)
            .creationDateTime(LocalDateTime.now())
            .logDetails(writeAsObjectNode(DocumentLogDetails.from(document)))
            .build();
        tenantLogCommonService.saveTenantLog(log);
    }

    public void addReprocessTenantLog(Long tenantId, Long operatorId, int documentCount) {
        TenantLog log = TenantLog.builder()
            .logType(LogType.ACCOUNT_REPROCESSED)
            .tenantId(tenantId)
            .operatorId(operatorId)
            .creationDateTime(LocalDateTime.now())
            .logDetails(writeAsObjectNode(Map.of("documentCount", documentCount)))
            .build();
        tenantLogCommonService.saveTenantLog(log);
    }

    public void addDeleteFileLog(Long tenantId, Long operatorId, File file) {
        TenantLog log = TenantLog.builder()
            .logType(LogType.FILE_DELETED)
            .tenantId(tenantId)
            .operatorId(operatorId)
            .creationDateTime(LocalDateTime.now())
            .logDetails(writeAsObjectNode(FileLogDetails.from(file)))
            .build();
        tenantLogCommonService.saveTenantLog(log);
    }

    public void addUpdateAmountLog(Long tenantId, Long operatorId, Document document, Integer newSum) {
        TenantLog log = TenantLog.builder()
            .logType(LogType.ACCOUNT_EDITED)
            .tenantId(tenantId)
            .operatorId(operatorId)
            .creationDateTime(LocalDateTime.now())
            .logDetails(writeAsObjectNode(UpdateMonthlySum.from(document, newSum)))
            .build();
        tenantLogCommonService.saveTenantLog(log);
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
