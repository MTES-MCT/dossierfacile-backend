package fr.dossierfacile.process.file.service.documentrules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.util.TwoDDocUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

class PublicPayslipRulesValidationServiceTest {

    private final PublicPayslipRulesValidationService publicPayslipRVS = new PublicPayslipRulesValidationService();

    private File buildValidDfFile(LocalDate date) throws JsonProcessingException {
        BarCodeFileAnalysis barCodeFileAnalysis = BarCodeFileAnalysis.builder()
                .verifiedData(new ObjectMapper()
                        .readValue("""
                                            {
                                                "Fin de période": "{startHex}", 
                                                "Début de période": "{endHex}",
                                                "Salaire net imposable": "3000,05",
                                                "SIRET de l’employeur": "123456789000001",
                                                "Date de début de contrat": "11111111",
                                                "Cumul du salaire net imposable": "30000,10",
                                                "Ligne 1 de la norme adresse postale du bénéficiaire de la prestation": "MR KALOUF JEAN"
                                            }
                                        """.replace("{startHex}", TwoDDocUtil.get2DDocHexDateFromLocalDate(date.with(TemporalAdjusters.firstDayOfMonth())))
                                        .replace("{endHex}", TwoDDocUtil.get2DDocHexDateFromLocalDate(date.with(TemporalAdjusters.lastDayOfMonth()))),
                                ObjectNode.class))
                .barCodeType(BarCodeType.TWO_D_DOC)
                .documentType(BarCodeDocumentType.PUBLIC_PAYSLIP)
                .build();

        PayslipFile parsedFile = PayslipFile.builder()
                .classification(ParsedFileClassification.PUBLIC_PAYSLIP)
                .fullname("MR KALOUF JEAN")
                .month(YearMonth.from(date))
                .netTaxableIncome(3000.05)
                .cumulativeNetTaxableIncome(30000.10)
                .build();

        ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder().parsedFile(parsedFile)
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.PUBLIC_PAYSLIP)
                .build();

        return File.builder()
                .fileAnalysis(barCodeFileAnalysis)
                .parsedFileAnalysis(parsedFileAnalysis)
                .build();
    }


    Document buildPublicPayslipDocument() throws JsonProcessingException {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("KALOUF")
                .build();

        LocalDate currentDate = LocalDate.now();
        return Document.builder()
                .tenant(tenant)
                .monthlySum(3000)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .files(Arrays.asList(buildValidDfFile(currentDate.minusMonths(1)),
                        buildValidDfFile(currentDate.minusMonths(2)),
                        buildValidDfFile(currentDate.minusMonths(3))))
                .build();
    }

    Document buildPublicPayslipDocumentWithWrongMonths() throws JsonProcessingException {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("KALOUF")
                .build();

        LocalDate currentDate = LocalDate.now();
        return Document.builder()
                .tenant(tenant)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .files(Arrays.asList(buildValidDfFile(currentDate.minusMonths(3)),
                        buildValidDfFile(currentDate.minusMonths(4)),
                        buildValidDfFile(currentDate.minusMonths(5))))
                .build();
    }

    @Test
    void document_full_test_ok() throws Exception {
        Document document = buildPublicPayslipDocument();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        publicPayslipRVS.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_full_test_wrong_month() throws Exception {
        Document document = buildPublicPayslipDocumentWithWrongMonths();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        publicPayslipRVS.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getBrokenRules()).hasSize(1);
        Assertions.assertThat(report.getBrokenRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_PAYSLIP_MONTHS);
    }

    @Test
    void document_full_test_wrong_average_amount() throws Exception {
        Document document = buildPublicPayslipDocument();
        document.setMonthlySum(2400);
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        publicPayslipRVS.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getBrokenRules()).hasSize(1);
        Assertions.assertThat(report.getBrokenRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES);
    }
}