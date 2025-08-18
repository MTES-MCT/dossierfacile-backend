package fr.dossierfacile.process.file.service.documentrules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.util.TwoDDocUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedList;
import java.util.List;

public class PublicPayslipRulesValidationServiceTest {

    private final PublicPayslipRulesValidationService service = new PublicPayslipRulesValidationService();
    private final ObjectMapper mapper = new ObjectMapper();

    private List<YearMonth> expectedPrimaryMonths() {
        LocalDate today = LocalDate.now();
        YearMonth now = YearMonth.now();
        if (today.getDayOfMonth() <= 15) {
            return List.of(now.minusMonths(1), now.minusMonths(2), now.minusMonths(3));
        } else {
            return List.of(now, now.minusMonths(1), now.minusMonths(2));
        }
    }

    private File publicPayslip(YearMonth ym, double net, double cumulative, String fullname, boolean withQr, String qrFullnameOverride, Double qrNetOverride, Double qrCumulativeOverride, YearMonth qrMonthOverride) {
        PayslipFile pf = PayslipFile.builder()
                .classification(ParsedFileClassification.PUBLIC_PAYSLIP)
                .fullname(fullname)
                .month(ym)
                .netTaxableIncome(net)
                .cumulativeNetTaxableIncome(cumulative)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.PUBLIC_PAYSLIP)
                .parsedFile(pf)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        if (withQr) {
            String qrFullname = qrFullnameOverride != null ? qrFullnameOverride : fullname;
            YearMonth qrMonth = qrMonthOverride != null ? qrMonthOverride : ym;
            double qrNet = qrNetOverride != null ? qrNetOverride : net;
            double qrCum = qrCumulativeOverride != null ? qrCumulativeOverride : cumulative;
            LocalDate dateMid = qrMonth.atDay(15);
            String hex = TwoDDocUtil.get2DDocHexDateFromLocalDate(dateMid);
            ObjectNode node = mapper.createObjectNode();
            node.put(TwoDDocDataType.ID_10.getLabel(), qrFullname);
            node.put(TwoDDocDataType.ID_54.getLabel(), hex);
            node.put(TwoDDocDataType.ID_58.getLabel(), String.valueOf(qrNet));
            node.put(TwoDDocDataType.ID_59.getLabel(), String.valueOf(qrCum));
            BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                    .verifiedData(node)
                    .barCodeType(BarCodeType.TWO_D_DOC)
                    .file(f)
                    .build();
            f.setFileAnalysis(bar);
        }
        return f;
    }

    private Document baseDocument(List<File> files, Integer monthlySum) {

        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
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
                .failedRules(new LinkedList<>())
                .passedRules(new LinkedList<>())
                .inconclusiveRules(new LinkedList<>())
                .build();
    }

    private File missingParsedFile() { return File.builder().build(); }

    @Test
    void shouldBeApplied_true_public_classification() {
        List<YearMonth> months = expectedPrimaryMonths();
        List<File> files = months.stream().map(m -> publicPayslip(m,2000,6000,"JEAN DUPONT", true,null,null,null,null)).toList();
        Document doc = baseDocument(files,2000);
        Assertions.assertThat(service.shouldBeApplied(doc)).isTrue();
    }

    @Test
    void shouldBeApplied_false_no_public_payslip() {
        // classification PAYSLIP au lieu de PUBLIC_PAYSLIP
        PayslipFile pf = PayslipFile.builder().classification(ParsedFileClassification.PAYSLIP).fullname("A B").month(YearMonth.now()).build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder().parsedFile(pf).classification(ParsedFileClassification.PAYSLIP).analysisStatus(ParsedFileAnalysisStatus.COMPLETED).build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        Document doc = baseDocument(List.of(f), 1000);
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void process_all_pass() {
        List<YearMonth> months = expectedPrimaryMonths();
        List<File> files = months.stream().map(m -> publicPayslip(m,2000,6000,"JEAN DUPONT", true,null,null,null,null)).toList();
        Document doc = baseDocument(files,2000);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getFailedRules()).isEmpty();
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_PAYSLIP_PARSING,
                        DocumentRule.R_PAYSLIP_QRCHECK,
                        DocumentRule.R_PAYSLIP_NAME,
                        DocumentRule.R_PAYSLIP_MONTHS,
                        DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES
                );
    }

    @Test
    void process_qr_fail_but_continue() {
        List<YearMonth> months = expectedPrimaryMonths();
        // Introduire mismatch fullname au 1er fichier => QR rule false
        File bad = publicPayslip(months.get(0),2000,6000,"JEAN DUPONT", true,"PAUL DURAND",null,null,null);
        File ok2 = publicPayslip(months.get(1),2000,6000,"JEAN DUPONT", true,null,null,null,null);
        File ok3 = publicPayslip(months.get(2),2000,6000,"JEAN DUPONT", true,null,null,null,null);
        Document doc = baseDocument(List.of(bad, ok2, ok3),2000);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_PAYSLIP_PARSING,
                        DocumentRule.R_PAYSLIP_NAME,
                        DocumentRule.R_PAYSLIP_MONTHS,
                        DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES
                );
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_PAYSLIP_QRCHECK);
    }

    @Test
    void process_amount_fail() {
        List<YearMonth> months = expectedPrimaryMonths();
        List<File> files = months.stream().map(m -> publicPayslip(m,2000,6000,"JEAN DUPONT", true,null,null,null,null)).toList();
        // monthlySum très différent pour faire échouer la dernière règle (>20%)
        Document doc = baseDocument(files, 3000);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES);
    }

    @Test
    void process_inconclusive_first_rule_blocks_rest() {
        List<YearMonth> months = expectedPrimaryMonths();
        // second fichier sans parsed -> règle parsing inconclusive blocking
        File ok = publicPayslip(months.get(0),2000,6000,"JEAN DUPONT", true,null,null,null,null);
        File missing = missingParsedFile();
        Document doc = baseDocument(List.of(ok, missing),2000);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getInconclusiveRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_PAYSLIP_PARSING);
        Assertions.assertThat(report.getPassedRules()).isEmpty();
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }
}

