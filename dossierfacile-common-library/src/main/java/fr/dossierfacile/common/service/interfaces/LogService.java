package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.LogType;

public interface LogService {

    void saveLog(LogType logType, Long tenantId);

    void saveLogWithTenantData(LogType logType, Tenant tenant);
}
