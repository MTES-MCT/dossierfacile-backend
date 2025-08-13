package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public abstract class AbstractPayslipRulesValidationService implements RulesValidationService {
    protected abstract ParsedFileClassification getPayslipClassification();

    protected boolean checkQRCode(Document document) {
        return true;
    }

    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.FINANCIAL
                && document.getDocumentSubCategory() == DocumentSubCategory.SALARY
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getParsedFileAnalysis() != null
                && f.getParsedFileAnalysis().getParsedFile() != null
                && f.getParsedFileAnalysis().getParsedFile().getClassification() == getPayslipClassification());
    }

    protected boolean checkNamesRule(Document document) {
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(() -> document.getGuarantor());
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            PayslipFile parsedFile = (PayslipFile) analysis.getParsedFile();

            String fullName = parsedFile.getFullname().toUpperCase().replaceFirst("^(M. |MR |MME |MLLE |MONSIEUR |MADAME |MADEMOISELLE )", "");

            if (!PersonNameComparator.bearlyMixedEqualsTo(fullName, documentOwner.getLastName(), documentOwner.getFirstName())
                    && !PersonNameComparator.bearlyMixedEqualsTo(fullName, documentOwner.getPreferredName(), documentOwner.getFirstName())) {
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
                        List.of(yearMonth.minusMonths(2), yearMonth.minusMonths(3), yearMonth.minusMonths(4))) :
                List.of(
                        List.of(yearMonth, yearMonth.minusMonths(1), yearMonth.minusMonths(2)),
                        List.of(yearMonth.minusMonths(1), yearMonth.minusMonths(2), yearMonth.minusMonths(3)));
    }

    private boolean checkMonthsValidityRule(Document document) {
        List<List<YearMonth>> expectedMonthsList = getExpectedMonthsLists();

        List<YearMonth> presentMonths = document.getFiles().stream()
                .map(file -> ((PayslipFile) file.getParsedFileAnalysis().getParsedFile()).getMonth())
                .toList();

        return expectedMonthsList.stream().anyMatch(
                expectedMonths -> expectedMonths.stream().allMatch(month -> presentMonths.contains(month))
        );
    }

    private boolean checkAmountValidityRule(Document document) {
        List<PayslipFile> recentFiles = document.getFiles().stream()
                .map(file -> (PayslipFile) file.getParsedFileAnalysis().getParsedFile())
                .sorted(Comparator.comparing(PayslipFile::getMonth).reversed())
                .limit(3)
                .collect(Collectors.toList());

        double monthlyAverage = recentFiles.stream()
                .mapToDouble(PayslipFile::getNetTaxableIncome)
                .sum() / recentFiles.size();

        // Check percentage difference
        double diffPercentage = Math.abs((monthlyAverage - document.getMonthlySum()) / document.getMonthlySum());
        return (diffPercentage <= 0.2);
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

            if (checkQRCode(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_PAYSLIP_QRCHECK));
            } else {
                log.info("Document mismatch to QR CODE :{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_PAYSLIP_QRCHECK));
            }

            if (checkNamesRule(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_PAYSLIP_NAME));
            } else {
                log.info("Document names mismatches :{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_PAYSLIP_NAME));
            }

            if (checkMonthsValidityRule(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_PAYSLIP_MONTHS));
            } else {
                log.info("Document is expired :{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_PAYSLIP_MONTHS));
            }

            if (checkAmountValidityRule(document)) {
                report.addDocumentPassedRule(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES));
            } else {
                log.info("Document amount mismatch :{}", document.getId());
                report.addDocumentFailedRule(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES));
            }

        } catch (Exception e) {
            log.error("Error during the rules validation execution pocess", e);
            report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);
        }
        return report;
    }

}
