package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.common.application.projection.BaseProjection;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import org.springframework.stereotype.Component;

/**
 * Root projection, shared by the full and light views: the variant differences
 * (document names, dossier URLs) are already resolved in the ApplicationReadView.
 */
@Component
public class ApplicationProjection extends BaseProjection<ApplicationReadView, ApplicationModel> {

    private final TenantProjection tenantProjection;

    public ApplicationProjection(TenantProjection tenantProjection) {
        this.tenantProjection = tenantProjection;
    }

    @Override
    public ApplicationModel project(ApplicationReadView view) {
        ApplicationModel model = ApplicationModel.builder()
                .id(view.apartmentSharing().getId())
                .applicationType(view.apartmentSharing().getApplicationType())
                .dossierPdfDocumentStatus(view.apartmentSharing().getDossierPdfDocumentStatus())
                .status(view.aggregatedStatus())
                .lastUpdateDate(view.lastUpdateDate())
                .tenants(tenantProjection.projectAll(view.tenants()))
                .build();
        model.setDossierPdfUrl(view.dossierPdfUrl());
        model.setDossierUrl(view.dossierUrl());
        return model;
    }
}
