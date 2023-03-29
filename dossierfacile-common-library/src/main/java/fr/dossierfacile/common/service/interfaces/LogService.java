package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.enums.LogType;

public interface LogService {

    void saveLog(LogType logType, Long tenantId);

}
