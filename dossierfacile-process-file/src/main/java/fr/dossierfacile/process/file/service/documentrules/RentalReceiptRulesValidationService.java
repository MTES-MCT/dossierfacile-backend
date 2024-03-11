package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalReceiptRulesValidationService implements RulesValidationService {
    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.RESIDENCY
                && document.getDocumentSubCategory() == DocumentSubCategory.TENANT
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getParsedFileAnalysis() != null
                && f.getParsedFileAnalysis().getParsedFile() != null);
    }

    /**
     * Checks if names on each analysed file matches with the associated guarantor or to the tenant (more his partner for a couple).
     */
    private boolean checkNamesRule(Document document) {

        List<? extends Person> users = (document.getTenant() == null) ?
                List.of(document.getGuarantor()) :
                (document.getTenant().getApartmentSharing().getApplicationType() == ApplicationType.COUPLE) ?
                        document.getTenant().getApartmentSharing().getTenants() : List.of(document.getTenant());

        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            RentalReceiptFile parsedFile = (RentalReceiptFile) analysis.getParsedFile();

            String fullName = parsedFile.getTenantFullName().toUpperCase().replaceFirst("^(M. |MR |MME |MLLE |MONSIEUR |MADAME |MADEMOISELLE )", "");

            if (users.stream().noneMatch((user) -> PersonNameComparator.bearlyEqualsTo(fullName, user.getLastName(), user.getFirstName())
                    || PersonNameComparator.bearlyEqualsTo(fullName, user.getPreferredName(), user.getFirstName()))) {
                return false;
            }
        }
        return true;
    }

    private List<List<YearMonth>> getExpectedMonthsLists() {
        LocalDate localDate = LocalDate.now();
        YearMonth yearMonth = YearMonth.now();
        return (localDate.getDayOfMonth() <= 15) ?
                List.of(
                        List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3)),
                        List.of(yearMonth.minusMonths(2), yearMonth.minusMonths(3), yearMonth.minusMonths(4)),
                        List.of(yearMonth.minusMonths(3), yearMonth.minusMonths(4), yearMonth.minusMonths(5))) :
                List.of(
                        List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3)),
                        List.of(yearMonth.minusMonths(2), yearMonth.minusMonths(3), yearMonth.minusMonths(4)));
    }

    private List<List<YearMonth>> getGuarantorExpectedMonthsLists() {
        LocalDate localDate = LocalDate.now();
        YearMonth yearMonth = YearMonth.now();
        return (localDate.getDayOfMonth() <= 15) ?
                List.of(
                        List.of(yearMonth.minusMonths(1)),
                        List.of(yearMonth.minusMonths(2)),
                        List.of(yearMonth.minusMonths(3))) :
                List.of(
                        List.of(yearMonth.minusMonths(1)),
                        List.of(yearMonth.minusMonths(2)));
    }

    private boolean checkMonthsValidityRule(Document document) {
        List<List<YearMonth>> expectedMonthsList = (document.getTenant() != null) ? getExpectedMonthsLists() : getGuarantorExpectedMonthsLists();

        List<YearMonth> presentMonths = document.getFiles().stream()
                .map(file -> ((RentalReceiptFile) file.getParsedFileAnalysis().getParsedFile()).getPeriod())
                .toList();

        return expectedMonthsList.stream().anyMatch(
                expectedMonths -> expectedMonths.stream().allMatch(month -> presentMonths.contains(month))
        );
    }

    private boolean checkAddressWithSalary(Document document) {
        return true;
        // TODO currently neither payslip nor rentalReceipt has address
        /*
        // Get all payslip with data
        List<PublicPayslipFile> payslipFiles = document.getTenant().getDocuments().stream()
                .filter(d -> d.getDocumentSubCategory() == DocumentSubCategory.SALARY)
                .filter(d -> !CollectionUtils.isEmpty(d.getFiles()))
                .flatMap(d -> d.getFiles().stream())
                .filter(f -> f.getParsedFileAnalysis() != null)
                .map(f -> f.getParsedFileAnalysis())
                .filter(a -> a.getParsedFile() != null && a.getParsedFile() instanceof PublicPayslipFile)
                .map(a -> (PublicPayslipFile) a.getParsedFile())
                .toList();

        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            RentalReceiptFile parsedFile = (RentalReceiptFile) analysis.getParsedFile();

            payslipFiles.stream().anyMatch( payslip -> AddressComparator.compare(payslip.getAddress(), parsedFile.getTenantAddress() );
        }
*/
    }

    @Override
    public DocumentAnalysisReport process(Document document, DocumentAnalysisReport report) {

        try {
            if (CollectionUtils.isEmpty(document.getFiles()) || document.getFiles().stream()
                    .anyMatch(f -> f.getParsedFileAnalysis() == null || f.getParsedFileAnalysis().getParsedFile() == null)
            ) {
                report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
                return report;
            }

            if (!checkNamesRule(document)) {
                log.error("Document names mismatches :" + document.getId());
                report.getBrokenRules().add(DocumentBrokenRule.builder()
                        .rule(DocumentRule.R_RENT_RECEIPT_NAME)
                        .message(DocumentRule.R_RENT_RECEIPT_NAME.getDefaultMessage())
                        .build());
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else if (!checkMonthsValidityRule(document)) {
                log.error("Document is expired :" + document.getId());
                report.getBrokenRules().add(DocumentBrokenRule.builder()
                        .rule(DocumentRule.R_RENT_RECEIPT_MONTHS)
                        .message(DocumentRule.R_RENT_RECEIPT_MONTHS.getDefaultMessage())
                        .build());
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else if (!checkAddressWithSalary(document)) {
                log.error("Document with wrong address :" + document.getId());
                report.getBrokenRules().add(DocumentBrokenRule.builder()
                        .rule(DocumentRule.R_RENT_RECEIPT_ADDRESS_SALARY)
                        .message(DocumentRule.R_RENT_RECEIPT_ADDRESS_SALARY.getDefaultMessage())
                        .build());
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else {
                report.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
            }

        } catch (Exception e) {
            log.error("Error during the rules validation execution pocess", e);
            report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
        }
        return report;
    }

}