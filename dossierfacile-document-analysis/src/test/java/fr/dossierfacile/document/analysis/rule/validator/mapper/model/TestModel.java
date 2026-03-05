package fr.dossierfacile.document.analysis.rule.validator.mapper.model;

import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class TestModel {

    @DocumentIAField(extractionName = "string_value", type = DocumentIAPropertyType.STRING)
    private String stringValue;

    @DocumentIAField(extractionName = "date_value", type = DocumentIAPropertyType.DATE)
    private LocalDate dateValue;

    @DocumentIAField(extractionName = "list", type = DocumentIAPropertyType.LIST_STRING)
    private String[] list;

    @DocumentIAField(extractionName = "inner_model", type = DocumentIAPropertyType.OBJECT)
    private TestInnerModel testInnerModel;

    @DocumentIAField(extractionName = "list_inner_model", type = DocumentIAPropertyType.LIST_OBJECT)
    private List<TestInnerModel> listTestInnerModels;
}
