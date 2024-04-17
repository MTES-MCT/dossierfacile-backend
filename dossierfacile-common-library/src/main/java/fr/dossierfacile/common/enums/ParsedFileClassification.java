package fr.dossierfacile.common.enums;

import fr.dossierfacile.common.entity.FranceIdentiteApiResult;
import fr.dossierfacile.common.entity.ocr.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ParsedFileClassification {
    TAX_INCOME(TaxIncomeMainFile.class),
    TAX_INCOME_LEAF(TaxIncomeLeaf.class),
    GUARANTEE_PROVIDER(GuaranteeProviderFile.class),
    PUBLIC_PAYSLIP(PayslipFile.class),
    PAYSLIP(PayslipFile.class),
    SCHOLARSHIP(ScholarshipFile.class),
    RENTAL_RECEIPT(RentalReceiptFile.class),
    FRANCE_IDENTITE(FranceIdentiteApiResult.class);

    Class<? extends ParsedFile> classificationClass;
}
