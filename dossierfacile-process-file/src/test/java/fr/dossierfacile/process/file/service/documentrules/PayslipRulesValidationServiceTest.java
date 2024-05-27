package fr.dossierfacile.process.file.service.documentrules;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Arrays;
import java.util.LinkedList;

public class PayslipRulesValidationServiceTest {
    private final PayslipStandardRulesValidationService payslipRVS = new PayslipStandardRulesValidationService();

    private File buildValidDfFile(LocalDate date) throws JsonProcessingException {
        PayslipFile parsedFile = PayslipFile.builder()
                .classification(ParsedFileClassification.PAYSLIP)
                .fullname("Madame Brigitte DE LA MARCHE")
                .month(YearMonth.from(date))
                .netTaxableIncome(1000.05)
                .cumulativeNetTaxableIncome(10000.10)
                .build();

        ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder().parsedFile(parsedFile)
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.PAYSLIP)
                .build();

        return File.builder()
                .parsedFileAnalysis(parsedFileAnalysis)
                .build();
    }


    Document buildPayslipDocument() throws JsonProcessingException {
        Tenant tenant = Tenant.builder()
                .firstName("Brigitte")
                .lastName("DE LA MARCHE")
                .build();

        LocalDate currentDate = LocalDate.now();
        return Document.builder()
                .tenant(tenant)
                .monthlySum(1000)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SALARY)
                .files(Arrays.asList(buildValidDfFile(currentDate.minusMonths(1)),
                        buildValidDfFile(currentDate.minusMonths(2)),
                        buildValidDfFile(currentDate.minusMonths(3))))
                .build();
    }

    @Test
    void document_full_test_ok() throws Exception {
        Document document = buildPayslipDocument();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        payslipRVS.process(document, report);
        report.getBrokenRules().forEach((r) -> System.out.println(r.getMessage()));
        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_full_test_ko_wrong_name() throws Exception {
        Document document = buildPayslipDocument();
        document.getTenant().setLastName("LE BEAU");

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        payslipRVS.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        report.getBrokenRules().forEach((r) -> Assertions.assertThat(r.getRule()).isEqualTo(DocumentRule.R_PAYSLIP_NAME));
    }
}
