package fr.dossierfacile.process.file.service.documentrules;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;

class ScholarshipRulesValidationServiceTest {

    private final ScholarshipRulesValidationService scholarValidationService = new ScholarshipRulesValidationService();

    private File buildValidDfFile(LocalDate date) {
        int endYear = date.isBefore(LocalDate.of(date.getYear(), 9, 15)) ? date.getYear() : date.getYear() + 1;

        ScholarshipFile parsedFile = ScholarshipFile.builder()
                .firstName("Tom")
                .lastName("Mme Sawyer")
                .startYear(endYear - 1)
                .endYear(endYear)
                .annualAmount(10000)
                .build();

        ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder().parsedFile(parsedFile)
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.SCHOLARSHIP)
                .build();

        return File.builder()
                .parsedFileAnalysis(parsedFileAnalysis)
                .build();
    }


    Document buildDocument() throws JsonProcessingException {
        Tenant tenant = Tenant.builder()
                .firstName("Tom")
                .lastName("Sawyer")
                .build();

        return Document.builder()
                .tenant(tenant)
                .monthlySum(1000)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SCHOLARSHIP)
                .files(Collections.singletonList(buildValidDfFile(LocalDate.now())))
                .build();
    }

    Document buildDocumentWithWrongYear() {
        Tenant tenant = Tenant.builder()
                .firstName("Tom")
                .lastName("Sawyer")
                .build();

        LocalDate currentDate = LocalDate.now().minusYears(1);
        return Document.builder()
                .tenant(tenant)
                .monthlySum(1000)
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.SCHOLARSHIP)
                .files(Collections.singletonList(buildValidDfFile(currentDate)))
                .build();
    }

    @Test
    void document_full_test_ok() throws Exception {
        Document document = buildDocument();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        scholarValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_full_test_wrong_year() {
        Document document = buildDocumentWithWrongYear();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        scholarValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getBrokenRules()).hasSize(1);
        Assertions.assertThat(report.getBrokenRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_SCHOLARSHIP_EXPIRED);
    }

    @Test
    void document_full_test_wrong_average_amount() throws Exception {
        Document document = buildDocument();
        document.setMonthlySum(1020);
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        scholarValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getBrokenRules()).hasSize(1);
        Assertions.assertThat(report.getBrokenRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_SCHOLARSHIP_AMOUNT);
    }
}