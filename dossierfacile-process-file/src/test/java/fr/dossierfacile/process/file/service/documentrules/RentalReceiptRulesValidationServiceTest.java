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
import java.util.LinkedList;
import java.util.List;

class RentalReceiptRulesValidationServiceTest {

    private final RentalReceiptRulesValidationService validationService = new RentalReceiptRulesValidationService();

    private File buildValidDfFile(LocalDate period, LocalDate paymentDate) throws JsonProcessingException {

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
        Document doc = Document.builder()
                .tenant(tenant)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .files(Arrays.asList(buildValidDfFile(currentDate.minusMonths(2), currentDate),
                        buildValidDfFile(currentDate.minusMonths(3), currentDate),
                        buildValidDfFile(currentDate.minusMonths(4), currentDate)))
                .build();
        return doc;
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

    @Test
    void document_full_test_ok() throws Exception {
        Document document = buildRentalReceiptDocument();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        validationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_full_test_wrong_month() throws Exception {
        Document document = buildRentalReceiptDocumentWithWrongMonths();

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        validationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getBrokenRules()).hasSize(1);
        Assertions.assertThat(report.getBrokenRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_RENT_RECEIPT_MONTHS);
    }

}