package fr.dossierfacile.api.front.application.projection;

import fr.dossierfacile.api.front.application.projection.ApplicationReadView.GuarantorReadView;
import fr.dossierfacile.common.application.projection.BaseProjection;
import fr.dossierfacile.common.domain.model.guarantor.Guarantor;
import fr.dossierfacile.common.model.apartment_sharing.GuarantorModel;
import org.springframework.stereotype.Component;

@Component
public class GuarantorProjection extends BaseProjection<GuarantorReadView, GuarantorModel> {

    private final DocumentProjection documentProjection;

    public GuarantorProjection(DocumentProjection documentProjection) {
        this.documentProjection = documentProjection;
    }

    @Override
    public GuarantorModel project(GuarantorReadView view) {
        Guarantor guarantor = view.guarantor();
        return GuarantorModel.builder()
                .id(guarantor.getId())
                .firstName(guarantor.getFirstName())
                .lastName(guarantor.getLastName())
                .legalPersonName(guarantor.getLegalPersonName())
                .typeGuarantor(guarantor.getTypeGuarantor())
                .documents(documentProjection.projectAll(view.documents()))
                .build();
    }
}
