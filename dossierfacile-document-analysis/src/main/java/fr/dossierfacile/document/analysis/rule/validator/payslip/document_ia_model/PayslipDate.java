package fr.dossierfacile.document.analysis.rule.validator.payslip.document_ia_model;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAField;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper.DocumentIAModel;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@DocumentIAModel(documentCategory = DocumentCategory.FINANCIAL, documentSubCategory = DocumentSubCategory.SALARY)
public class PayslipDate {

    @DocumentIAField(twoDDocName = "debut_periode", extractionName = "periode_debut")
    public LocalDate startDate;

    @DocumentIAField(twoDDocName = "fin_periode", extractionName = "periode_fin")
    public LocalDate endDate;

    @DocumentIAField(extractionName = "date_paiement")
    public LocalDate paymentDate;

}
