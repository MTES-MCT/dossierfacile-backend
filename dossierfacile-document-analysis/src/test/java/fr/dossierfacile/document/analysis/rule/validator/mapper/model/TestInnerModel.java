package fr.dossierfacile.document.analysis.rule.validator.mapper.model;

import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TestInnerModel {

    @DocumentIAField(extractionName = "inner_string")
    private String innerString;

    @DocumentIAField(extractionName = "inner_date")
    private LocalDate innerDate;

    @DocumentIAField(extractionName = "inner_list")
    private String[] innerList;
}
