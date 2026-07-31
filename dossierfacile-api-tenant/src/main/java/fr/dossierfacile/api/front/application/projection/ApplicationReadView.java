package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.document.Document;
import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.enums.TenantFileStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Composed read views: each level carries its aggregate plus everything the projection
 * needs that the aggregate itself cannot provide — neighbouring aggregates (documents,
 * guarantors) and derived values (effective status, displayed date, resolved document
 * names). A view is complete by construction: projections never compute, only map.
 *
 * The full/light difference is entirely resolved at view-building time (document name
 * strategy, dossier URLs), so a single projection hierarchy serves both.
 */
public record ApplicationReadView(
        ApartmentSharing apartmentSharing,
        List<TenantReadView> tenants,
        TenantFileStatus aggregatedStatus,
        LocalDateTime lastUpdateDate,
        String dossierPdfUrl,
        String dossierUrl
) {

    public record TenantReadView(
            Tenant tenant,
            TenantFileStatus status,
            List<DocumentReadView> documents,
            List<GuarantorReadView> guarantors
    ) {
    }

    public record GuarantorReadView(
            Guarantor guarantor,
            List<DocumentReadView> documents
    ) {
    }

    public record DocumentReadView(
            Document document,
            String name
    ) {
    }

    /**
     * Structural assembly only: walks the loaded sources and resolves each document name
     * with the given strategy. No business rule lives here — statuses and dates are read
     * as-is from the sources.
     */
    public static ApplicationReadView from(ApplicationProjectionSources sources,
                                           Function<Document, String> documentName,
                                           String dossierPdfUrl,
                                           String dossierUrl) {
        List<TenantReadView> tenants = sources.tenants().stream()
                .map(tenant -> new TenantReadView(
                        tenant,
                        sources.statusByTenantId().get(tenant.getId()),
                        toDocumentViews(sources.documentsByTenantId().getOrDefault(tenant.getId(), List.of()), documentName),
                        sources.guarantorsByTenantId().getOrDefault(tenant.getId(), List.of()).stream()
                                .map(guarantor -> new GuarantorReadView(
                                        guarantor,
                                        toDocumentViews(sources.documentsByGuarantorId().getOrDefault(guarantor.getId(), List.of()), documentName)))
                                .toList()))
                .toList();

        return new ApplicationReadView(
                sources.apartmentSharing(),
                tenants,
                sources.aggregatedStatus(),
                sources.lastUpdateDate(),
                dossierPdfUrl,
                dossierUrl);
    }

    private static List<DocumentReadView> toDocumentViews(List<Document> documents,
                                                          Function<Document, String> documentName) {
        return documents.stream()
                .map(document -> new DocumentReadView(document, documentName.apply(document)))
                .toList();
    }
}
