package fr.dossierfacile.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.mapper.DeletedTenantCommonMapper;
import fr.dossierfacile.common.model.EditedDocumentModel;
import fr.dossierfacile.common.repository.LogRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@AllArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogRepository repository;
    private final DeletedTenantCommonMapper deletedTenantCommonMapper;
    private ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.registerModule(new JavaTimeModule());
    }

    private void saveLog(Log log) {
        repository.save(log);
    }

    @Override
    public void saveLog(LogType logType, Long tenantId) {
        this.saveLog(new Log(logType, tenantId));
    }

    @Override
    public void saveLogWithTenantData(LogType logType, Tenant tenant) {
        String content = null;
        try {
            content = objectMapper.writeValueAsString(deletedTenantCommonMapper.toDeletedTenantModel(tenant));
        } catch (Exception e) {
            log.error("Cannot correclty record tenant information in tenant_log");
        }
        this.saveLog(
                Log.builder()
                        .logType(logType)
                        .tenantId(tenant.getId())
                        .creationDateTime(LocalDateTime.now())
                        .userApis(tenant.getTenantsUserApi().stream()
                                .mapToLong(tenantUserApi -> tenantUserApi.getUserApi().getId()).toArray())
                        .jsonProfile(content)
                        .build()
        );
    }

    @Override
    public void saveDocumentEditedLog(Document document, Tenant editor) {
        Log log = Log.builder()
                .logType(LogType.ACCOUNT_EDITED)
                .tenantId(editor.getId())
                .creationDateTime(LocalDateTime.now())
                .logDetails(writeDocumentEditionDetails(document, editor.getId()))
                .build();
        saveLog(log);
    }

    private String writeDocumentEditionDetails(Document document, Long tenantId) {
        try {
            return objectMapper.writeValueAsString(EditedDocumentModel.from(document));
        } catch (JsonProcessingException e) {
            log.error("Cannot write details of document edition from tenant {}", tenantId);
        }
        return null;
    }

}
