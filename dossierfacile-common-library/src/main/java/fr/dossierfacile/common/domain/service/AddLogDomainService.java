package fr.dossierfacile.common.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.infrastructure.entity.FileEntity;
import fr.dossierfacile.common.model.log.DocumentLogDetails;
import fr.dossierfacile.common.model.log.FileLogDetails;
import fr.dossierfacile.common.repository.TenantLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddLogDomainService {

    private final TenantLogRepository repository;
    private final ObjectMapper objectMapper;

    public void addDocumentDeletedLog(Document document, Tenant editor) {
        DocumentLogDetails details = DocumentLogDetails.builder()
                .documentCategory(document.getDocumentCategory())
                .documentSubCategory(document.getDocumentSubCategory())
                .documentId(document.getId())
                .tenantId(document.getTenantId())
                .guarantorId(document.getGuarantorId())
                .build();

        TenantLog logEntity = TenantLog.builder()
                .logType(LogType.DOCUMENT_DELETED)
                .tenantId(editor.getId())
                .creationDateTime(LocalDateTime.now())
                .logDetails(writeAsObjectNode(details))
                .build();
        repository.save(logEntity);
    }

    public void addFileDeletedLog(FileEntity file, Tenant editor) {
        TenantLog logEntity = TenantLog.builder()
                .logType(LogType.FILE_DELETED)
                .tenantId(editor.getId())
                .creationDateTime(LocalDateTime.now())
                .logDetails(writeAsObjectNode(FileLogDetails.from(file)))
                .build();
        repository.save(logEntity);
    }

    private ObjectNode writeAsObjectNode(Object object) {
        try {
            return objectMapper.valueToTree(object);
        } catch (IllegalArgumentException e) {
            log.error("FATAL: Cannot write log details as object node", e);
        }
        return null;
    }
}
