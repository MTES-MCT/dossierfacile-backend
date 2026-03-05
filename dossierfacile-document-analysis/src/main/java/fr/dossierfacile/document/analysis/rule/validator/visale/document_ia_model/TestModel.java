package fr.dossierfacile.document.analysis.rule.validator.visale.document_ia_model;

import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TestModel {

    @DocumentIAField(extractionName = "string_value", type = DocumentIAPropertyType.STRING)
    private String stringValue;

    @DocumentIAField(extractionName = "date_value", type = DocumentIAPropertyType.DATE)
    private LocalDate dateValue;

    @DocumentIAField(extractionName = "list", type = DocumentIAPropertyType.LIST_STRING)
    private String[] list;
}
