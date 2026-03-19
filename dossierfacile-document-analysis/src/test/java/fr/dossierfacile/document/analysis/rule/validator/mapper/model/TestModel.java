package fr.dossierfacile.document.analysis.rule.validator.mapper.model;

import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class TestModel {

    @DocumentIAField(extractionName = "string_value")
    private String stringValue;

    @DocumentIAField(extractionName = "date_value")
    private LocalDate dateValue;

    @DocumentIAField(extractionName = "list")
    private String[] list;

    @DocumentIAField(extractionName = "inner_model")
    private TestInnerModel testInnerModel;

    @DocumentIAField(extractionName = "list_inner_model")
    private List<TestInnerModel> listTestInnerModels;
}
