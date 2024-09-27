package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.Owner;

public interface OwnerCommonService {
    Owner findByKeycloakId(String keycloakId);
}
