package fr.dossierfacile.common.enums;

import fr.dossierfacile.common.entity.ocr.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ParsedFileClassification {
    TAX_INCOME(TaxIncomeMainFile.class),
    TAX_INCOME_LEAF(TaxIncomeLeaf.class),
    GUARANTEE_PROVIDER(GuaranteeProviderFile.class),
    PUBLIC_PAYSLIP(PublicPayslipFile.class),
    RENTAL_RECEIPT(RentalReceiptFile.class);

    Class<? extends ParsedFile> classificationClass;
}
