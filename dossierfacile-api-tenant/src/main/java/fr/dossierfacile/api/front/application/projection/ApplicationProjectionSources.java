package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Aggregates loaded for the application read model (full/light views).
 */
public record ApplicationProjectionSources(
        ApartmentSharing apartmentSharing,
        List<Tenant> tenants,
        Map<Long, List<Guarantor>> guarantorsByTenantId,
        Map<Long, List<Document>> documentsByTenantId,
        Map<Long, List<Document>> documentsByGuarantorId,
        Map<Long, TenantFileStatus> statusByTenantId,
        TenantFileStatus aggregatedStatus,
        LocalDateTime lastUpdateDate
) {
}
