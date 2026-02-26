package fr.dossierfacile.document.analysis.rule.validator.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

@DocumentIAModel(
        documentCategory = DocumentCategory.IDENTIFICATION,
        documentSubCategory = DocumentSubCategory.FRENCH_IDENTITY_CARD
)
public class TestIdentityTwoDDocModel {

    @DocumentIAField(extractionName = "nom_test", type = DocumentIAPropertyType.STRING)
    private String lastName;

    @DocumentIAField(twoDDocName = "doc_type", type = DocumentIAPropertyType.STRING)
    private String docType;

    @DocumentIAField(twoDDocName = "reference_avis", type = DocumentIAPropertyType.STRING)
    private String referenceAvis;
}

