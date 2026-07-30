package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.enums.TenantFileStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes the aggregated status of an apartment sharing from its tenants' effective statuses.
 * Exact replica of the legacy in-memory computation (common/entity/ApartmentSharing#getStatus):
 * the evaluation order is part of the contract.
 */
@Service
public class ApartmentSharingStatusDomainService {

    public TenantFileStatus computeStatus(List<TenantFileStatus> tenantStatuses) {
        for (TenantFileStatus status : tenantStatuses) {
            if (status == TenantFileStatus.DECLINED) {
                return TenantFileStatus.DECLINED;
            }
        }
        for (TenantFileStatus status : tenantStatuses) {
            if (status == TenantFileStatus.INCOMPLETE) {
                return TenantFileStatus.INCOMPLETE;
            }
        }
        int archivedTenantCount = 0;
        for (TenantFileStatus status : tenantStatuses) {
            if (status == TenantFileStatus.ARCHIVED) {
                archivedTenantCount++;
            }
        }
        if (archivedTenantCount > 0) {
            if (archivedTenantCount == tenantStatuses.size()) {
                return TenantFileStatus.ARCHIVED;
            }
            return TenantFileStatus.INCOMPLETE;
        }
        for (TenantFileStatus status : tenantStatuses) {
            if (status == TenantFileStatus.TO_PROCESS) {
                return TenantFileStatus.TO_PROCESS;
            }
        }
        return TenantFileStatus.VALIDATED;
    }
}
