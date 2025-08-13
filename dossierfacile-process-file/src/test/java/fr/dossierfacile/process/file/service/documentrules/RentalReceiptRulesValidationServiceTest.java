package fr.dossierfacile.process.file.service.documentrules;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.common.enums.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class RentalReceiptRulesValidationServiceTest {

    private final RentalReceiptRulesValidationService validationService = new RentalReceiptRulesValidationService();

    private File buildValidDfFile(LocalDate period, LocalDate paymentDate) {

        RentalReceiptFile parsedFile = RentalReceiptFile.builder()
                .period(YearMonth.from(period))
                .tenantFullName("DUPONT JEAN")
                .ownerFullName("MOULIN JEAN")
                .amount(500.0)
                .paymentDate(paymentDate)
                .build();

        ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder().parsedFile(parsedFile)
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.RENTAL_RECEIPT)
                .build();

        return File.builder()
                .parsedFileAnalysis(parsedFileAnalysis)
                .numberOfPages(1)
                .build();
    }


    Document buildRentalReceiptDocument() throws JsonProcessingException {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("DUPONT")
                .build();
        ApartmentSharing as = ApartmentSharing.builder().tenants(List.of(tenant)).build();
        as.setApplicationType(ApplicationType.ALONE);
        tenant.setApartmentSharing(as);

        LocalDate currentDate = LocalDate.now().with(ChronoField.DAY_OF_MONTH, 14);
        return Document.builder()
                .tenant(tenant)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .files(Arrays.asList(buildValidDfFile(currentDate.minusMonths(2), currentDate),
                        buildValidDfFile(currentDate.minusMonths(3), currentDate),
                        buildValidDfFile(currentDate.minusMonths(4), currentDate)))
                .build();
    }

    Document buildRentalReceiptDocumentWithWrongMonths() throws JsonProcessingException {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("DUPONT")
                .build();
        ApartmentSharing as = ApartmentSharing.builder().tenants(List.of(tenant)).build();
        as.setApplicationType(ApplicationType.ALONE);
        tenant.setApartmentSharing(as);

        LocalDate currentDate = LocalDate.now();
        return Document.builder()
                .tenant(tenant)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .files(Arrays.asList(buildValidDfFile(currentDate.minusMonths(4), currentDate),
                        buildValidDfFile(currentDate.minusMonths(5), currentDate),
                        buildValidDfFile(currentDate.minusMonths(6), currentDate)))
                .build();
    }

    Document buildRentalReceiptDocumentWithOneMonth() throws JsonProcessingException {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("DUPONT")
                .build();
        ApartmentSharing as = ApartmentSharing.builder().tenants(List.of(tenant)).build();
        as.setApplicationType(ApplicationType.ALONE);
        tenant.setApartmentSharing(as);

        LocalDate currentDate = LocalDate.now();
        return Document.builder()
                .tenant(tenant)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .files(Collections.singletonList(buildValidDfFile(currentDate.minusMonths(1), currentDate)))
                .build();
    }

    @Test
    void document_full_test_ok() throws Exception {
        Document document = buildRentalReceiptDocument();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .failedRules(new LinkedList<>())
                .build();
        validationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_full_test_wrong_month() throws Exception {
        Document document = buildRentalReceiptDocumentWithWrongMonths();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .failedRules(new LinkedList<>())
                .build();
        validationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getFailedRules()).hasSize(1);
        Assertions.assertThat(report.getFailedRules().getFirst()).matches(docRule -> docRule.getRule() == DocumentRule.R_RENT_RECEIPT_MONTHS);
    }

    @Test
    void when_only_one_month_then_report_error() throws Exception {
        Document document = buildRentalReceiptDocumentWithOneMonth();
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .failedRules(new LinkedList<>())
                .build();
        validationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getFailedRules()).hasSize(2);
        Assertions.assertThat( report.getFailedRules().stream().anyMatch(docRule -> docRule.getRule() == DocumentRule.R_RENT_RECEIPT_NB_DOCUMENTS)).isTrue();
    }
}