package fr.dossierfacile.document.analysis.rule.validator.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;

import java.util.List;

@DocumentIAModel(
        documentCategory = DocumentCategory.IDENTIFICATION,
        documentSubCategory = DocumentSubCategory.FRENCH_IDENTITY_CARD
)
public class TestIdentityNestedCollectionsModel {

    @DocumentIAField(extractionName = "tags")
    private String[] tags;

    @DocumentIAField(extractionName = "holder")
    private Holder holder;

    @DocumentIAField(extractionName = "beneficiaries")
    private List<Beneficiary> beneficiaries;

    static class Holder {
        @DocumentIAField(extractionName = "first_name")
        private String firstName;
    }

    static class Beneficiary {
        @DocumentIAField(extractionName = "first_name")
        private String firstName;

        @DocumentIAField(extractionName = "inner_tags")
        private String[] innerTags;
    }
}
