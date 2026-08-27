package fr.dossierfacile.document.analysis.rule.validator.professional;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalClassificationRuleBITest {

    private final ProfessionalClassificationRuleBI rule = new ProfessionalClassificationRuleBI();

    @Test
    void should_pass_when_at_least_one_barcode_has_doc_type_29() {
        // Given
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .build();

        Document document = buildDocumentWithBarcodes(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(output.isBlocking()).isTrue();
    }

    @Test
    void should_pass_when_multiple_barcodes_and_one_is_doc_type_29() {
        // Given - First barcode non-29, second barcode type 29
        BarcodeModel barcode1 = BarcodeModel.builder().docType("10").build();
        BarcodeModel barcode2 = BarcodeModel.builder().docType("29").build();

        Document document = buildDocumentWithBarcodes(List.of(barcode1, barcode2));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(output.isBlocking()).isTrue();
    }

    @Test
    void should_be_inconclusive_and_blocking_when_no_barcode_has_doc_type_29() {
        // Given
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("27")
                .build();

        Document document = buildDocumentWithBarcodes(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        assertThat(output.isBlocking()).isTrue();
    }

    @Test
    void should_be_inconclusive_and_blocking_when_doc_type_is_null_or_empty() {
        // Given
        BarcodeModel barcode1 = BarcodeModel.builder().docType(null).build();
        BarcodeModel barcode2 = BarcodeModel.builder().docType("").build();

        Document document = buildDocumentWithBarcodes(List.of(barcode1, barcode2));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        assertThat(output.isBlocking()).isTrue();
    }

    @Test
    void should_be_inconclusive_and_blocking_when_no_successful_analysis() {
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
        assertThat(output.isBlocking()).isTrue();
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
