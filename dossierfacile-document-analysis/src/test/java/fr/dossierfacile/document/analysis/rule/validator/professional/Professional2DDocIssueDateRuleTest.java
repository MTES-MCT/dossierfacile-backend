package fr.dossierfacile.document.analysis.rule.validator.professional;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Professional2DDocIssueDateRuleTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-08-27T12:00:00Z"), ZoneId.of("UTC"));
    private final Professional2DDocIssueDateRule rule = new Professional2DDocIssueDateRule(fixedClock);

    @Test
    void should_pass_when_issue_date_is_within_2_months() {
        // Given - 1 month ago (within 2 months limit)
        LocalDate recentDate = LocalDate.now(fixedClock).minusMonths(1);
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .issueDate(recentDate)
                .build();

        Document document = buildDocumentWithBarcodes(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    void should_pass_when_issue_date_is_today() {
        // Given
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .issueDate(LocalDate.now(fixedClock))
                .build();

        Document document = buildDocumentWithBarcodes(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    void should_pass_when_issue_date_is_exactly_2_months_ago() {
        // Given - exactly 2 months ago (boundary case)
        LocalDate boundaryDate = LocalDate.now(fixedClock).minusMonths(2);
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .issueDate(boundaryDate)
                .build();

        Document document = buildDocumentWithBarcodes(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    void should_be_failed_when_issue_date_is_older_than_2_months() {
        // Given - 3 months ago (exceeds 2 months limit)
        LocalDate oldDate = LocalDate.now(fixedClock).minusMonths(3);
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .issueDate(oldDate)
                .build();

        Document document = buildDocumentWithBarcodes(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void should_be_failed_when_issue_date_is_in_the_future() {
        // Given - 2 days in the future
        LocalDate futureDate = LocalDate.now(fixedClock).plusDays(2);
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .issueDate(futureDate)
                .build();

        Document document = buildDocumentWithBarcodes(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void should_ignore_non_type_29_barcodes_and_evaluate_type_29_barcode() {
        // Given - First barcode is type "10" (non-PNDS) with an old issueDate; Second is type "29" (PNDS) with recent issueDate
        BarcodeModel nonType29Barcode = BarcodeModel.builder()
                .docType("10")
                .issueDate(LocalDate.now(fixedClock).minusMonths(6))
                .build();

        BarcodeModel type29Barcode = BarcodeModel.builder()
                .docType("29")
                .issueDate(LocalDate.now(fixedClock).minusMonths(1))
                .build();

        Document document = buildDocumentWithBarcodes(List.of(nonType29Barcode, type29Barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    void should_be_inconclusive_when_issue_date_is_null() {
        // Given
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .issueDate(null)
                .build();

        Document document = buildDocumentWithBarcodes(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    void should_be_inconclusive_when_no_successful_analysis() {
        // Given
        Document document = Document.builder()
                .files(List.of(
                        File.builder()
                                .documentIAFileAnalysis(DocumentIAFileAnalysis.builder()
                                        .analysisStatus(DocumentIAFileAnalysisStatus.FAILED)
                                        .build())
                                .build()
                ))
                .build();

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    private Document buildDocumentWithBarcodes(List<BarcodeModel> barcodes) {
        ResultModel resultModel = ResultModel.builder()
                .barcodes(barcodes)
                .build();

        DocumentIAFileAnalysis analysis = DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(resultModel)
                .build();

        File file = File.builder()
                .documentIAFileAnalysis(analysis)
                .build();

        return Document.builder()
                .files(List.of(file))
                .build();
    }
}
