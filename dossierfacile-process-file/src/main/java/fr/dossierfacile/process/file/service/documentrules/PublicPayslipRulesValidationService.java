package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.PublicPayslipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.util.PersonNameComparator;
import fr.dossierfacile.process.file.util.TwoDDocUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicPayslipRulesValidationService implements RulesValidationService {
    @Override
    public boolean shouldBeApplied(Document document) {
        return document.getDocumentCategory() == DocumentCategory.FINANCIAL
                && document.getDocumentSubCategory() == DocumentSubCategory.SALARY
                && !CollectionUtils.isEmpty(document.getFiles())
                && document.getFiles().stream().anyMatch((f) -> f.getParsedFileAnalysis() != null
                && f.getParsedFileAnalysis().getParsedFile() != null
                && f.getParsedFileAnalysis().getParsedFile().getClassification() == ParsedFileClassification.PUBLIC_PAYSLIP);
    }

    private PublicPayslipFile fromQR(BarCodeFileAnalysis barCodeFileAnalysis) {
        Map<String, String> dataWithLabel = (Map<String, String>) barCodeFileAnalysis.getVerifiedData();
        return PublicPayslipFile.builder()
                .fullname(dataWithLabel.get(TwoDDocDataType.ID_10.getLabel()))
                .month(YearMonth.from(TwoDDocUtil.getLocalDateFrom2DDocHexDate(dataWithLabel.get(TwoDDocDataType.ID_54.getLabel()))))
                .netTaxableIncome(Double.parseDouble(dataWithLabel.get(TwoDDocDataType.ID_58.getLabel()).replace(" ", "").replace(',', '.')))
                .cumulativeNetTaxableIncome(Double.parseDouble(dataWithLabel.get(TwoDDocDataType.ID_59.getLabel()).replace(" ", "").replace(',', '.')))
                .build();
    }

    private boolean checkQRCode(Document document) {
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            if (analysis == null || dfFile.getFileAnalysis() == null || analysis.getAnalysisStatus() == ParsedFileAnalysisStatus.FAILED) {
                continue;
            }
            if (analysis.getClassification() == ParsedFileClassification.PUBLIC_PAYSLIP) {
                PublicPayslipFile qrDocument = fromQR(dfFile.getFileAnalysis());
                PublicPayslipFile parsedDocument = (PublicPayslipFile) analysis.getParsedFile();

                if (qrDocument == null
                        || qrDocument.getFullname() == null
                        || qrDocument.getMonth() == null
                        || qrDocument.getCumulativeNetTaxableIncome() == 0
                        || !PersonNameComparator.equalsWithNormalization(qrDocument.getFullname(), parsedDocument.getFullname())
                        || !qrDocument.getMonth().equals(parsedDocument.getMonth())
                        || qrDocument.getNetTaxableIncome() != parsedDocument.getNetTaxableIncome()
                        || qrDocument.getCumulativeNetTaxableIncome() != parsedDocument.getCumulativeNetTaxableIncome()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkNamesRule(Document document) {
        Person documentOwner = Optional.ofNullable((Person) document.getTenant()).orElseGet(() -> document.getGuarantor());
        for (File dfFile : document.getFiles()) {
            ParsedFileAnalysis analysis = dfFile.getParsedFileAnalysis();
            PublicPayslipFile parsedFile = (PublicPayslipFile) analysis.getParsedFile();

            String fullname = parsedFile.getFullname().toUpperCase().replaceFirst("^(MR |MME |MLLE )", "");

            if (!PersonNameComparator.bearlyEqualsTo(fullname, documentOwner.getLastName(), documentOwner.getFirstName())
                    && !PersonNameComparator.bearlyEqualsTo(fullname, documentOwner.getPreferredName(), documentOwner.getFirstName())) {
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
                .map(file -> ((PublicPayslipFile) file.getParsedFileAnalysis().getParsedFile()).getMonth())
                .toList();

        return expectedMonthsList.stream().anyMatch(
                expectedMonths -> expectedMonths.stream().allMatch(month -> presentMonths.contains(month))
        );
    }

    private boolean checkAmountValidityRule(Document document) {
        List<PublicPayslipFile> recentFiles = document.getFiles().stream()
                .map(file -> (PublicPayslipFile) file.getParsedFileAnalysis().getParsedFile())
                .sorted(Comparator.comparing(PublicPayslipFile::getMonth).reversed())
                .limit(3)
                .collect(Collectors.toList());

        double monthlyAverage = recentFiles.stream()
                .mapToDouble(PublicPayslipFile::getNetTaxableIncome)
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

            if (!checkQRCode(document)) {
                log.error("Document mismatch to QR CODE :" + document.getId());
                report.getBrokenRules().add(DocumentBrokenRule.builder()
                        .rule(DocumentRule.R_PAYSLIP_QRCHECK)
                        .message(DocumentRule.R_PAYSLIP_QRCHECK.getDefaultMessage())
                        .build());
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else if (!checkNamesRule(document)) {
                log.error("Document names mismatches :" + document.getId());
                report.getBrokenRules().add(DocumentBrokenRule.builder()
                        .rule(DocumentRule.R_PAYSLIP_NAME)
                        .message(DocumentRule.R_PAYSLIP_NAME.getDefaultMessage())
                        .build());
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else if (!checkMonthsValidityRule(document)) {
                log.error("Document is expired :" + document.getId());
                report.getBrokenRules().add(DocumentBrokenRule.builder()
                        .rule(DocumentRule.R_PAYSLIP_MONTHS)
                        .message(DocumentRule.R_PAYSLIP_MONTHS.getDefaultMessage())
                        .build());
                report.setAnalysisStatus(DocumentAnalysisStatus.DENIED);
            } else if (!checkAmountValidityRule(document)) {
                log.error("Amount specified on document mismatch :" + document.getId());
                report.getBrokenRules().add(DocumentBrokenRule.builder()
                        .rule(DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES)
                        .message(DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES.getDefaultMessage())
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