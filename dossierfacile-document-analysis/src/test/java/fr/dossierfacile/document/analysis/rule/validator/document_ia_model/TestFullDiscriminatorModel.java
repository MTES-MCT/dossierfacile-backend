package fr.dossierfacile.document.analysis.rule.validator.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

@DocumentIAModel(
        documentCategory = DocumentCategory.FINANCIAL,
        documentCategoryStep = DocumentCategoryStep.RENT_ANNUITY_LIFE,
        documentSubCategory = DocumentSubCategory.CDI
)
public class TestFullDiscriminatorModel {
    @DocumentIAField(extractionName = "test", type = DocumentIAPropertyType.STRING)
    private String test;
}
