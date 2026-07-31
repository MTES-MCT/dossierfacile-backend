package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.api.front.application.projection.ApplicationReadView.TenantReadView;
import fr.dossierfacile.common.application.projection.BaseProjection;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.TenantModel;
import org.springframework.stereotype.Component;

@Component
public class TenantProjection extends BaseProjection<TenantReadView, TenantModel> {

    private final GuarantorProjection guarantorProjection;
    private final DocumentProjection documentProjection;

    public TenantProjection(GuarantorProjection guarantorProjection, DocumentProjection documentProjection) {
        this.guarantorProjection = guarantorProjection;
        this.documentProjection = documentProjection;
    }

    @Override
    public TenantModel project(TenantReadView view) {
        Tenant tenant = view.tenant();
        return TenantModel.builder()
                .id(tenant.getId())
                .firstName(tenant.getFirstName())
                .lastName(tenant.getLastName())
                .preferredName(tenant.getPreferredName())
                .zipCode(tenant.getZipCode())
                .abroad(tenant.getAbroad())
                .email(tenant.getEmail())
                .tenantType(tenant.getTenantType())
                .franceConnect(tenant.getFranceConnect())
                .ownerType(tenant.getOwnerType())
                .status(view.status())
                .honorDeclaration(tenant.getHonorDeclaration())
                .clarification(tenant.getClarification())
                .lastUpdateDate(tenant.getLastUpdateDate())
                .documents(documentProjection.projectAll(view.documents()))
                .guarantors(guarantorProjection.projectAll(view.guarantors()))
                .build();
    }
}
