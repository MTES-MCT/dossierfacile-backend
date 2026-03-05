package fr.dossierfacile.document.analysis.rule.validator.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

import java.util.List;

@DocumentIAModel(
        documentCategory = DocumentCategory.IDENTIFICATION,
        documentSubCategory = DocumentSubCategory.FRENCH_IDENTITY_CARD
)
public class TestIdentityNestedCollectionsModel {

    @DocumentIAField(extractionName = "tags", type = DocumentIAPropertyType.LIST_STRING)
    private String[] tags;

    @DocumentIAField(extractionName = "holder", type = DocumentIAPropertyType.OBJECT)
    private Holder holder;

    @DocumentIAField(extractionName = "beneficiaries", type = DocumentIAPropertyType.LIST_OBJECT)
    private List<Beneficiary> beneficiaries;

    static class Holder {
        @DocumentIAField(extractionName = "first_name", type = DocumentIAPropertyType.STRING)
        private String firstName;
    }

    static class Beneficiary {
        @DocumentIAField(extractionName = "first_name", type = DocumentIAPropertyType.STRING)
        private String firstName;

        @DocumentIAField(extractionName = "inner_tags", type = DocumentIAPropertyType.LIST_STRING)
        private String[] innerTags;
    }
}
