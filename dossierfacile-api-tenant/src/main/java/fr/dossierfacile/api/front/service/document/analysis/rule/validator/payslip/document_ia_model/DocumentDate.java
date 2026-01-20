package fr.dossierfacile.api.front.service.document.analysis.rule.validator.payslip.document_ia_model;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Setter;

import java.time.LocalDate;

@Setter
public class DocumentDate {

    @DocumentIAField(
            extractionName = "date_delivrance",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate deliveryDate;

    @DocumentIAField(
            extractionName = "date_expiration",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate expirationDate;

    @DocumentIAField(
            extractionName = "date_naissance",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate birthDate;

}
