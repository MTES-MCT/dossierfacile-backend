package fr.dossierfacile.api.front.service.document.analysis.rule.validator.payslip.document_ia_model;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.api.front.service.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import lombok.Setter;

import java.time.LocalDate;

@Setter
public class PayslipDate {

    @DocumentIAField(
            extractionName = "periode_debut",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate startDate;

    @DocumentIAField(
            extractionName = "periode_fin",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate endDate;

    @DocumentIAField(
            extractionName = "date_paiement",
            type = DocumentIAPropertyType.DATE
    )
    public LocalDate paymentDate;

}
