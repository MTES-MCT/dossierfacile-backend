package fr.dossierfacile.document.analysis.rule.validator.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

@DocumentIAModel(
        documentCategory = DocumentCategory.TAX,
        documentCategoryStep = DocumentCategoryStep.UNDEFINED,
        documentSubCategory = DocumentSubCategory.MY_NAME
)
public class TestSubCategoryDiscriminatorModel {
    @DocumentIAField(extractionName = "test", type = DocumentIAPropertyType.STRING)
    private String test;
}
