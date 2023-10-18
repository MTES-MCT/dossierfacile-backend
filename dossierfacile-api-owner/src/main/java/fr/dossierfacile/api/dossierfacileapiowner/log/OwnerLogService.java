package fr.dossierfacile.api.dossierfacileapiowner.log;

import fr.dossierfacile.common.entity.Owner;
import fr.dossierfacile.common.enums.OwnerLogType;

public interface OwnerLogService {
    void saveLog(OwnerLogType ownerLogType, Long OwnerId);

    void saveLogWithOwnerData(OwnerLogType ownerLogType, Owner owner);
}