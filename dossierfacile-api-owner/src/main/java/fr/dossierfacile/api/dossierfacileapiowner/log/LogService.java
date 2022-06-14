package fr.dossierfacile.api.dossierfacileapiowner.log;

import fr.dossierfacile.common.enums.LogType;

public interface LogService {

    void saveLog(LogType logType, Long id);

}
