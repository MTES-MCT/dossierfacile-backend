package fr.dossierfacile.document.analysis.rule.validator.payslip.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import lombok.Setter;

@Setter
@DocumentIAModel(documentCategory = DocumentCategory.FINANCIAL, documentSubCategory = DocumentSubCategory.SALARY)
public class PayslipNames {

    @DocumentIAField(twoDDocName = "beneficiaire")
    public BeneficiaireModel beneficiaire;

    @DocumentIAField(extractionName = "identite_salarie")
    public String identiteString;

    public String getIdentityString() {
        if (beneficiaire != null) {
            String fromBeneficiaire = beneficiaire.resolveIdentityString();
            if (fromBeneficiaire != null) return fromBeneficiaire;
        }
        return identiteString;
    }
}
