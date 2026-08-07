package fr.dossierfacile.common.mapper;

import fr.dossierfacile.common.enums.TenantFileStatus;

/**
 * MapStruct mixin for the owner-facing mappers, whose readers are always external:
 * implementing it makes every mapped {@link TenantFileStatus} field (enum or String
 * form) go through the defensive COMPLETED masking, unconditionally.
 */
public interface MasksCompletedStatusForOwner {

    default TenantFileStatus toOwnerVisibleTenantStatus(TenantFileStatus status) {
        return PartnerVisibleStatus.mask(status, getClass().getSimpleName());
    }

    default String toOwnerVisibleStatus(TenantFileStatus status) {
        if (status == null) {
            return null;
        }
        return PartnerVisibleStatus.mask(status, getClass().getSimpleName()).name();
    }
}
