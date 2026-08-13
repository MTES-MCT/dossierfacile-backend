package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.enums.TenantFileStatus;
import org.mapstruct.Context;

/**
 * MapStruct mixin for mappers serving both the tenant itself and partners:
 * implementing it makes every mapped {@link TenantFileStatus} field go through the
 * defensive COMPLETED masking when a partner context is present. The tenant's own
 * views (userApi == null) keep seeing the real status.
 */
public interface MasksCompletedStatusForPartner {

    default TenantFileStatus toPartnerVisibleStatus(TenantFileStatus status, @Context UserApi userApi) {
        if (userApi == null) {
            return status;
        }
        return PartnerVisibleStatus.mask(status, getClass().getSimpleName());
    }
}
