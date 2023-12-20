package fr.dossierfacile.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.mapper.DeletedTenantCommonMapper;
import fr.dossierfacile.common.model.log.ApplicationTypeChange;
import fr.dossierfacile.common.model.log.EditedDocument;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.repository.LogRepository;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
        this.saveLog(
                Log.builder()
                        .logType(logType)
                        .tenantId(tenant.getId())
                        .creationDateTime(LocalDateTime.now())
                        .userApis(tenant.getTenantsUserApi().stream()
                                .mapToLong(tenantUserApi -> tenantUserApi.getUserApi().getId()).toArray())
                        .jsonProfile(writeAsString(deletedTenantCommonMapper.toDeletedTenantModel(tenant)))
                        .build()
        );
    }

    @Override
    public void saveDocumentEditedLog(Document document, Tenant editor, EditionType editionType) {
        Log log = Log.builder()
                .logType(LogType.ACCOUNT_EDITED)
                .tenantId(editor.getId())
                .creationDateTime(LocalDateTime.now())
                .logDetails(writeAsString(EditedDocument.from(document, editionType)))
                .build();
        saveLog(log);
    }

    @Override
    public void savePartnerAccessRevocationLog(Tenant tenant, UserApi userApi) {
        Log log = Log.builder()
                .logType(LogType.PARTNER_ACCESS_REVOKED)
                .tenantId(tenant.getId())
                .creationDateTime(LocalDateTime.now())
                .userApis(new long[] { userApi.getId() })
                .build();
        saveLog(log);
    }

    @Override
    public void saveApplicationTypeChangedLog(List<Tenant> tenants, ApplicationType oldType, ApplicationType newType) {
        if (oldType == newType) {
            return;
        }
        for (Tenant tenant : tenants) {
            Log log = Log.builder()
                    .logType(LogType.APPLICATION_TYPE_CHANGED)
                    .tenantId(tenant.getId())
                    .creationDateTime(LocalDateTime.now())
                    .logDetails(writeAsString(new ApplicationTypeChange(oldType, newType)))
                    .build();
            saveLog(log);
        }
    }

    private String writeAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Cannot write log details as string");
        }
        return null;
    }

}
