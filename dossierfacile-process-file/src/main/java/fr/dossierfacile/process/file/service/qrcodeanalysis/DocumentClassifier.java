package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;

import static fr.dossierfacile.common.enums.DocumentCategory.FINANCIAL;
import static fr.dossierfacile.common.enums.DocumentCategory.PROFESSIONAL;
import static fr.dossierfacile.common.enums.DocumentCategory.RESIDENCY;
import static fr.dossierfacile.common.enums.DocumentCategory.TAX;
import static fr.dossierfacile.common.enums.DocumentSubCategory.GUEST;
import static fr.dossierfacile.common.enums.DocumentSubCategory.GUEST_PARENTS;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.RETIRED;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SALARY;

@AllArgsConstructor
public class DocumentClassifier {

    private final BarCodeDocumentType documentType;

    public boolean isCompatibleWith(File file) {
        return isCompatibleWith(file.getDocument());
    }

    public boolean isCompatibleWith(Document document) {
        DocumentSlot documentSlot = new DocumentSlot(document.getDocumentCategory(), document.getDocumentSubCategory(), document.getGuarantor() != null);
        return switch (documentType) {
            case TAX_ASSESSMENT -> documentSlot.canReceiveTaxAssessment();
            case TAX_DECLARATION, CVEC -> false;
            case PAYFIT_PAYSLIP, SNCF_PAYSLIP, PUBLIC_PAYSLIP, THALES_PAYSLIP, UNKNOWN_PAYSLIP -> documentSlot.canReceivePayslip();
            case FREE_INVOICE -> documentSlot.canReceiveInvoice();
            case UNKNOWN -> true;
        };
    }

    @AllArgsConstructor
    private static class DocumentSlot {

        private final DocumentCategory category;
        private final DocumentSubCategory subCategory;
        private final boolean isGuarantor;

        private boolean canReceiveTaxAssessment() {
            if (category == TAX && subCategory == MY_NAME) {
                return true;
            }
            if (isGuarantor) {
                return (category == PROFESSIONAL && subCategory == RETIRED)
                        || category == FINANCIAL;
            }
            return false;
        }

        private boolean canReceivePayslip() {
            if (category == FINANCIAL && subCategory == SALARY) {
                return true;
            }
            if (isGuarantor) {
                return category == PROFESSIONAL;
            }
            return false;
        }

        private boolean canReceiveInvoice() {
            if (category == RESIDENCY) {
                return subCategory == GUEST || subCategory == GUEST_PARENTS;
            }
            return false;
        }

    }

}
