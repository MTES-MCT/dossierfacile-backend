package fr.dossierfacile.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.OwnerLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.OwnerLogType;
import fr.dossierfacile.common.mapper.log.DeletedOwnerMapper;
import fr.dossierfacile.common.mapper.log.DeletedTenantMapper;
import fr.dossierfacile.common.model.log.ApplicationTypeChange;
import fr.dossierfacile.common.model.log.EditedDocument;
import fr.dossierfacile.common.model.log.EditedStep;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.repository.OwnerLogCommonRepository;
import fr.dossierfacile.common.repository.TenantLogRepository;
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

    private final TenantLogRepository repository;
    private final OwnerLogCommonRepository ownerLogRepository;
    private final DeletedTenantMapper deletedTenantMapper;
    private final DeletedOwnerMapper deletedOwnerMapper;
    private final ObjectMapper objectMapper;

    private void saveLog(TenantLog log) {
        repository.save(log);
    }

    @Override
    public void saveLog(LogType logType, Long tenantId) {
        this.saveLog(new TenantLog(logType, tenantId));
    }

    @Override
    public void saveStepLog(Long tenantId, String stepName) {
        TenantLog log = TenantLog.builder()
                .logType(LogType.ACCOUNT_EDITED)
                .tenantId(tenantId)
                .creationDateTime(LocalDateTime.now())
                .logDetails(writeAsString(new EditedStep(stepName)))
                .build();
        saveLog(log);
    }

    @Override
    public void saveLogWithOwnerData(OwnerLogType logType, Owner owner) {
        ownerLogRepository.save(
                OwnerLog.builder()
                        .logType(logType)
                        .ownerId(owner.getId())
                        .creationDateTime(LocalDateTime.now())
                        .jsonProfile(writeAsString(deletedOwnerMapper.toDeletedOwnerModel(owner)))
                        .build()
        );
    }

    @Override
    public void saveLogWithTenantData(LogType logType, Tenant tenant) {
        this.saveLog(
                TenantLog.builder()
                        .logType(logType)
                        .tenantId(tenant.getId())
                        .creationDateTime(LocalDateTime.now())
                        .userApis(tenant.getTenantsUserApi().stream()
                                .mapToLong(tenantUserApi -> tenantUserApi.getUserApi().getId()).toArray())
                        .jsonProfile(writeAsString(deletedTenantMapper.toDeletedTenantModel(tenant)))
                        .build()
        );
    }

    @Override
    public void saveDocumentEditedLog(Document document, Tenant editor, EditionType editionType) {
        TenantLog log = TenantLog.builder()
                .logType(LogType.ACCOUNT_EDITED)
                .tenantId(editor.getId())
                .creationDateTime(LocalDateTime.now())
                .logDetails(writeAsString(EditedDocument.from(document, editionType)))
                .build();
        saveLog(log);
    }

    @Override
    public void savePartnerAccessRevocationLog(Tenant tenant, UserApi userApi) {
        TenantLog log = TenantLog.builder()
                .logType(LogType.PARTNER_ACCESS_REVOKED)
                .tenantId(tenant.getId())
                .creationDateTime(LocalDateTime.now())
                .userApis(new long[]{userApi.getId()})
                .build();
        saveLog(log);
    }

    @Override
    public void saveApplicationTypeChangedLog(List<Tenant> tenants, ApplicationType oldType, ApplicationType newType) {
        if (oldType == newType) {
            return;
        }
        for (Tenant tenant : tenants) {
            TenantLog log = TenantLog.builder()
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
            log.error("FATAL: Cannot write log details as string", e);
        }
        return null;
    }

}
