package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.api.front.repository.ApiTenantLogRepository;
import fr.dossierfacile.common.application.exception.ModelNotFoundException;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.domain.service.ApartmentSharingStatusDomainService;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService.DocumentView;
import fr.dossierfacile.common.domain.service.UpdateTenantStatusDomainService.GuarantorView;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaGuarantorRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads every aggregate needed to project an application (full/light views).
 * Must be called within an active transaction (the use case's executeInTransaction).
 */
@Component
@RequiredArgsConstructor
public class ApplicationProjectionLoader {

    private final JpaApartmentSharingRepository jpaApartmentSharingRepository;
    private final JpaTenantRepository jpaTenantRepository;
    private final JpaGuarantorRepository jpaGuarantorRepository;
    private final JpaDocumentRepository jpaDocumentRepository;
    private final ApartmentSharingStatusDomainService apartmentSharingStatusDomainService;
    private final ApiTenantLogRepository tenantLogRepository;

    public ApplicationProjectionSources load(Long apartmentSharingId) {
        ApartmentSharing apartmentSharing = jpaApartmentSharingRepository.findById(apartmentSharingId)
                .orElseThrow(() -> new ModelNotFoundException(ApartmentSharing.class, apartmentSharingId));

        List<Tenant> tenants = jpaTenantRepository.findAllByApartmentSharingId(apartmentSharingId);
        List<Long> tenantIds = tenants.stream().map(Tenant::getId).toList();

        List<Guarantor> guarantors = jpaGuarantorRepository.findAllByTenantIds(tenantIds);
        Map<Long, List<Guarantor>> guarantorsByTenantId = guarantors.stream()
                .collect(Collectors.groupingBy(Guarantor::getTenantId));

        Map<Long, List<Document>> documentsByTenantId = jpaDocumentRepository.getDocumentsByTenantsIds(tenantIds).stream()
                .sorted(Comparator.comparing(Document::getId))
                .collect(Collectors.groupingBy(Document::getTenantId));

        List<Long> guarantorIds = guarantors.stream().map(Guarantor::getId).toList();
        Map<Long, List<Document>> documentsByGuarantorId = jpaDocumentRepository.getDocumentsByGuarantorsIds(guarantorIds).stream()
                .sorted(Comparator.comparing(Document::getId))
                .collect(Collectors.groupingBy(Document::getGuarantorId));

        // Effective status: pre-"status" rows have a null value;
        // the legacy getter computed it on the fly — replicate that fallback here,
        // reusing the already-loaded documents and guarantors (no extra query)
        Map<Long, TenantFileStatus> statusByTenantId = new LinkedHashMap<>();
        for (Tenant tenant : tenants) {
            TenantFileStatus status = tenant.getStatus() != null
                    ? tenant.getStatus()
                    : fallbackStatus(
                            tenant,
                            documentsByTenantId.getOrDefault(tenant.getId(), List.of()),
                            guarantorsByTenantId.getOrDefault(tenant.getId(), List.of()),
                            documentsByGuarantorId);
            statusByTenantId.put(tenant.getId(), status);
        }

        TenantFileStatus aggregatedStatus = apartmentSharingStatusDomainService.computeStatus(
                List.copyOf(statusByTenantId.values()));

        return new ApplicationProjectionSources(
                apartmentSharing,
                tenants,
                guarantorsByTenantId,
                documentsByTenantId,
                documentsByGuarantorId,
                statusByTenantId,
                aggregatedStatus,
                lastUpdateDate(apartmentSharing, aggregatedStatus)
        );
    }

    private static TenantFileStatus fallbackStatus(Tenant tenant,
                                                   List<Document> documents,
                                                   List<Guarantor> guarantors,
                                                   Map<Long, List<Document>> documentsByGuarantorId) {
        return UpdateTenantStatusDomainService.computeStatus(
                tenant.getStatus(),
                Boolean.TRUE.equals(tenant.getHonorDeclaration()),
                toDocumentViews(documents),
                guarantors.stream()
                        .map(guarantor -> new GuarantorView(
                                guarantor.getTypeGuarantor(),
                                toDocumentViews(documentsByGuarantorId.getOrDefault(guarantor.getId(), List.of()))))
                        .toList());
    }

    private static List<DocumentView> toDocumentViews(List<Document> documents) {
        return documents.stream()
                .map(document -> new DocumentView(document.getDocumentStatus(), document.getDocumentCategory()))
                .toList();
    }

    /**
     * Legacy rule: a VALIDATED application displays the date of its last ACCOUNT_VALIDATED log
     * instead of the raw lastUpdateDate.
     */
    private LocalDateTime lastUpdateDate(ApartmentSharing apartmentSharing, TenantFileStatus aggregatedStatus) {
        LocalDateTime lastUpdateDate = apartmentSharing.getLastUpdateDate();
        if (aggregatedStatus == TenantFileStatus.VALIDATED) {
            return tenantLogRepository.findLastValidationLogByApartmentSharing(apartmentSharing.getId())
                    .map(TenantLog::getCreationDateTime)
                    .orElse(lastUpdateDate);
        }
        return lastUpdateDate;
    }
}
