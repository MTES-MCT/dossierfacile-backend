package fr.dossierfacile.document.analysis.rule.validator.tax.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

// This model is used to sanitize the 2DDoc content of a Tax
@DocumentIAModel(documentCategory = DocumentCategory.TAX, documentSubCategory = DocumentSubCategory.MY_NAME)
public class Tax2DDocModel {

    @DocumentIAField(twoDDocName = "declarant_1")
    public String declarant1;

    @DocumentIAField(twoDDocName = "declarant_2")
    public String declarant2;

    @DocumentIAField(twoDDocName = "annee_des_revenus")
    public String anneeDesRevenus;
}
