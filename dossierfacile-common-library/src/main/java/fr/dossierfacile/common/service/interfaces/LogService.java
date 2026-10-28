package fr.dossierfacile.common.service.interfaces;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.enums.OwnerLogType;

import java.util.List;

public interface LogService {

    void saveLog(LogType logType, Long tenantId);

    /** Saves a tenant log of the given type with free-form JSON details. */
    void saveLogWithDetails(LogType logType, Long tenantId, ObjectNode details);

    void saveStepLog(Long tenantId, String step);

    void saveGuarantorNotifiedLog(Guarantor guarantor);

    void saveLogWithOwnerData(OwnerLogType logType, Owner owner);

    void saveLogWithTenantData(LogType logType, Tenant tenant);

    void saveDocumentAddedLog(Document document, Tenant editor);

    void saveDocumentDeletedLog(Document document, Tenant editor);

    void saveFileAddedLog(File file, Tenant editor);

    void saveFileDeletedLog(File file, Tenant editor);

    void savePartnerAccessRevocationLog(Tenant tenant, UserApi userApi);

    void saveApplicationTypeChangedLog(List<Tenant> tenants, ApplicationType oldType, ApplicationType newType);
}
