package fr.dossierfacile.document.analysis.rule.validator.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

@DocumentIAModel(
        documentCategory = DocumentCategory.IDENTIFICATION,
        documentSubCategory = DocumentSubCategory.FRENCH_IDENTITY_CARD
)
public class TestIdentityModel {

    @DocumentIAField(extractionName = "nom_test")
    private String lastName;

    @DocumentIAField(extractionName = "prenom_test")
    private String firstName;
}
