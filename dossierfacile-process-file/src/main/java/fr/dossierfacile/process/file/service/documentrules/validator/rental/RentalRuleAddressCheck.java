package fr.dossierfacile.process.file.service.documentrules.validator.rental;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.service.documentrules.validator.AbstractDocumentRuleValidator;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class RentalRuleAddressCheck extends AbstractDocumentRuleValidator {

    @Override
    protected boolean isBlocking() {
        return false;
    }

    @Override
    protected boolean isInconclusive() {
        return false;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_RENT_RECEIPT_ADDRESS_SALARY;
    }

    @Override
    protected boolean isValid(Document document) {
        // TODO currently neither payslip nor rentalReceipt has address
        /*List<PublicPayslipFile> payslipFiles = document.getTenant().getDocuments().stream()
                .filter(d -> d.getDocumentSubCategory() == DocumentSubCategory.SALARY)
                .filter(d -> !CollectionUtils.isEmpty(d.getFiles()))
                .flatMap(d -> d.getFiles().stream())
                .filter(f -> f.getParsedFileAnalysis() != null)
                .map(File::getParsedFileAnalysis)
                .filter(a -> a.getParsedFile() != null && a.getParsedFile() instanceof PublicPayslipFile)
                .map(a -> (PublicPayslipFile) a.getParsedFile())
                .toList();

        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            RentalReceiptFile parsedFile = (RentalReceiptFile) analysis.getParsedFile();

            payslipFiles.stream().anyMatch( payslip -> AddressComparator.compare(payslip.getAddress(), parsedFile.getTenantAddress() );
        }*/
        return true;
    }
}
