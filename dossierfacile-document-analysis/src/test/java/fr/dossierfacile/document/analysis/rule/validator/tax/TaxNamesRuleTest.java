package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaxNamesRuleTest {

    private final TaxNamesRule rule = new TaxNamesRule();

    @Test
    @DisplayName("Should validate when first declarant names match")
    void should_validate_matching_names() {
        Tenant tenant = Tenant.builder().lastName("DOE").firstName("JOHN").build();
        Document document = documentWithAnalysis(List.of(fakeAvisImposition("DOE MIKEAL JOHN")), tenant);

        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_NAMES);

        assertThat(result.rule().getExtractedDatas()).hasSize(1);
        assertThat(result.rule().getExtractedDatas().getFirst()).extracting("name", "value").containsExactly("document_1_declarant_1", "[MIKEAL, JOHN] DOE");
    }

    @Test
    @DisplayName("Should validate when second declarant matches")
    void should_validate_when_second_declarant_matches() {
        Tenant tenant = Tenant.builder().lastName("DOE").firstName("JANE").build();
        Document document = documentWithAnalysis(List.of(fakeAvisImposition( "DOE JOHN", "DOE JANE")), tenant);

        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);

        assertThat(result.rule().getExtractedDatas()).hasSize(2);
        assertThat(result.rule().getExtractedDatas())
                .extracting("value")
                .containsExactlyInAnyOrder("[JOHN] DOE", "[JANE] DOE");
    }


    @Test
    @DisplayName("Should fail when names do not match")
    void should_fail_mismatching_names() {
        Tenant tenant = Tenant.builder().lastName("SMITH").firstName("JOHN").build();
        Document document = documentWithAnalysis(List.of(fakeAvisImposition("DOE JANE")), tenant);

        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_NAMES);

        assertThat(result.rule().getExtractedDatas()).hasSize(1);
        assertThat(result.rule().getExtractedDatas().getFirst()).extracting("name", "value").containsExactly("document_1_declarant_1", "[JANE] DOE");
    }

    @Test
    @DisplayName("Should validate with multiple files if one matches")
    void should_validate_with_multiple_files_one_matching() {
        Tenant tenant = Tenant.builder().lastName("DOE").firstName("JOHN").build();
        Document document = documentWithAnalysis(
                List.of(
                        fakeAvisImposition("SMITH JANE"), // Mismatch
                        fakeAvisImposition("DOE JOHN")   // Match
                ),
                tenant
        );

        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);

        assertThat(result.rule().getExtractedDatas()).hasSize(2);
        assertThat(result.rule().getExtractedDatas())
                .extracting("value")
                .containsExactlyInAnyOrder("[JANE] SMITH", "[JOHN] DOE");
    }

    @Test
    @DisplayName("Should fail with multiple files if none match")
    void should_fail_with_multiple_files_no_match() {
        Tenant tenant = Tenant.builder().lastName("DOE").firstName("JOHN").build();
        Document document = documentWithAnalysis(
                List.of(
                        fakeAvisImposition("SMITH JANE"),
                        fakeAvisImposition("COOPER ALICE")
                ),
                tenant
        );

        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Should be inconclusive when no tax document found")
    void should_be_inconclusive_when_no_tax_document() {
        Tenant tenant = Tenant.builder().lastName("DOE").firstName("JOHN").build();
        Document document = documentWithAnalysis(List.of(fakeOtherDocument()), tenant);

        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    // ==========================================
    // Fixtures
    // ==========================================

    private Document documentWithAnalysis(List<DocumentIAFileAnalysis> analysiss, Tenant tenant) {
        List<File> files = analysiss.stream()
                .map(analysis -> File.builder()
                        .documentIAFileAnalysis(analysis)
                        .build())
                .toList();
        return Document.builder()
                .tenant(tenant)
                .files(files)
                .build();
    }

    private DocumentIAFileAnalysis fakeAvisImposition(String declarant1Name) {
        return this.fakeAvisImposition(declarant1Name, null);
    }

    private DocumentIAFileAnalysis fakeAvisImposition(String declarant1Name, String declarant2Name) {
        List<GenericProperty> typedData = List.of(
                GenericProperty.builder().name("doc_type").value("28").type("string").build(),
                GenericProperty.builder().name("declarant_1").value(declarant1Name).type("string").build(),
                GenericProperty.builder().name("declarant_2").value(declarant2Name).type("string").build()
        );

        BarcodeModel barcode = BarcodeModel.builder()
                .pageNumber(1)
                .type("2D_DOC")
                .isValid(true)
                .rawData(null)
                .typedData(typedData)
                .antsType("avis_imposition")
                .build();

        ResultModel result = ResultModel.builder()
                .barcodes(List.of(barcode))
                .build();

        return DocumentIAFileAnalysis.builder()
                .documentIaExecutionId("exec-id")
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private DocumentIAFileAnalysis fakeOtherDocument() {
        BarcodeModel barcode = BarcodeModel.builder()
                .type("2D_DOC")
                .isValid(true)
                .antsType("other_type")
                .build();
        ResultModel result = ResultModel.builder()
                .barcodes(List.of(barcode))
                .build();
        return DocumentIAFileAnalysis.builder()
                .documentIaExecutionId("exec-id")
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }
}
