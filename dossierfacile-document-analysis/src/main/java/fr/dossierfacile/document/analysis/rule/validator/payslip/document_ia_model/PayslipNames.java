package fr.dossierfacile.document.analysis.rule.validator.payslip.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.utils.IDocumentIdentity;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import lombok.Setter;

import java.util.List;

@Setter
@DocumentIAModel(documentCategory = DocumentCategory.FINANCIAL, documentSubCategory = DocumentSubCategory.SALARY)
public class PayslipNames implements IDocumentIdentity {

    @DocumentIAField(
            extractionName = "nom_salarie",
            type = DocumentIAPropertyType.STRING
    )
    public String lastName;

    @DocumentIAField(
            extractionName = "prenom_salarie",
            type = DocumentIAPropertyType.STRING
    )
    public String firstName;

    @Override
    public List<String> getFirstNames() {
        if (firstName != null) {
            return List.of(firstName);
        }
        return List.of();
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPreferredName() {
        return null;
    }
}
