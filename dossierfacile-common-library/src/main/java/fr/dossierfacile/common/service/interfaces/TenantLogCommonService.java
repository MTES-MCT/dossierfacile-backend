package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.QueueEntrySource;

public interface TenantLogCommonService {

    void saveTenantLog(TenantLog log);

    void logQueueEntered(Long tenantId, QueueEntrySource source);
}
