package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.model.log.EditionType;

import java.util.List;

public interface LogService {

    void saveLog(LogType logType, Long tenantId);

    void saveStepLog(Long tenantId, String step);

    void saveLogWithTenantData(LogType logType, Tenant tenant);

    void saveDocumentEditedLog(Document document, Tenant editor, EditionType editionType);

    void savePartnerAccessRevocationLog(Tenant tenant, UserApi userApi);

    void saveApplicationTypeChangedLog(List<Tenant> tenants, ApplicationType oldType, ApplicationType newType);

}
