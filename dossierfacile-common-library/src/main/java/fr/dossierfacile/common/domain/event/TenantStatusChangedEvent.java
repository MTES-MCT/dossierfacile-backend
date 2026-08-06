package fr.dossierfacile.common.domain.event;

import fr.dossierfacile.common.entity.User;
import fr.dossierfacile.common.enums.TenantFileStatus;

public record TenantStatusChangedEvent(
        Long tenantId,
        TenantFileStatus oldStatus,
        TenantFileStatus newStatus,
        User operator
) {
}
