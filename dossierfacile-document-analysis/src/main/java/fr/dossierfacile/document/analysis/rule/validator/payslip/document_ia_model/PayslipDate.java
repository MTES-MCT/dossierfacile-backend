package fr.dossierfacile.document.analysis.rule.validator.payslip.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.DocumentIAPropertyType;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@DocumentIAModel(documentCategory = DocumentCategory.FINANCIAL, documentSubCategory = DocumentSubCategory.SALARY)
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
