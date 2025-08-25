package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedList;
import java.util.List;

class PayslipStandardRulesValidationServiceTest {

    private final PayslipStandardRulesValidationService service = new PayslipStandardRulesValidationService();

    private List<YearMonth> expectedPrimaryMonths() {
        LocalDate today = LocalDate.now();
        YearMonth now = YearMonth.now();
        if (today.getDayOfMonth() <= 15) {
            return List.of(now.minusMonths(1), now.minusMonths(2), now.minusMonths(3));
        } else {
            return List.of(now, now.minusMonths(1), now.minusMonths(2));
        }
    }

    private File payslip(YearMonth ym, double net) {
        PayslipFile pf = PayslipFile.builder()
                .fullname("JEAN DUPONT")
                .month(ym)
                .netTaxableIncome(net)
                .cumulativeNetTaxableIncome(net)
                .classification(ParsedFileClassification.PAYSLIP)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(pf)
                .classification(ParsedFileClassification.PAYSLIP)
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File payslipMissingParsed() {
        return File.builder().build();
    }

    private Document baseDocument(List<File> files, Integer monthlySum) {

        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .preferredName("DURAND")
                .build();

        return Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .files(files)
                .tenant(tenant)
                .monthlySum(monthlySum)
                .build();
    }

    private DocumentAnalysisReport emptyReport(Document d) {
        return DocumentAnalysisReport.builder()
                .document(d)
                .passedRules(new LinkedList<>())
                .failedRules(new LinkedList<>())
                .inconclusiveRules(new LinkedList<>())
                .build();
    }

    @Test
    void shouldBeApplied_true_when_financial_salary_and_parsed_classification_matches() {
        List<YearMonth> months = expectedPrimaryMonths();
        Document doc = baseDocument(months.stream().map(m -> payslip(m, 2000)).toList(), 2000);
        Assertions.assertThat(service.shouldBeApplied(doc)).isTrue();
    }

    @Test
    void shouldBeApplied_false_wrong_category() {
        List<YearMonth> months = expectedPrimaryMonths();
        Document doc = Document.builder()
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .files(months.stream().map(m -> payslip(m, 2000)).toList())
                .build();
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void shouldBeApplied_false_no_files() {
        Document doc = Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .build();
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void process_all_rules_pass_standard() {
        List<YearMonth> months = expectedPrimaryMonths();
        List<File> files = months.stream().map(m -> payslip(m, 2000)).toList();
        Document doc = baseDocument(files, 2000);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getFailedRules()).isEmpty();
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_PAYSLIP_PARSING,
                        DocumentRule.R_PAYSLIP_QRCHECK, // trivially passed (no QR expected)
                        DocumentRule.R_PAYSLIP_NAME,
                        DocumentRule.R_PAYSLIP_MONTHS,
                        DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES
                );
    }

    @Test
    void process_blocks_after_first_blocking_inconclusive_when_missing_parsed() {
        List<YearMonth> months = expectedPrimaryMonths();
        List<File> files = List.of(payslip(months.get(0), 2000), payslipMissingParsed());
        Document doc = baseDocument(files, 2000);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getInconclusiveRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_PAYSLIP_PARSING);
        Assertions.assertThat(report.getPassedRules()).isEmpty();
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }
}

