package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.rule.TaxClassificationRuleData;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaxClassificationRuleBTest {

    private final TaxClassificationRuleB rule = new TaxClassificationRuleB();

    @Test
    @DisplayName("Only declarative situation should failed with a custom error")
    void should_failed_only_declarative_situation() {
        Document document = documentWithAnalysis(List.of(fakeAvisDeclaratif()));
        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_BAD_CLASSIFICATION);
        var data = (TaxClassificationRuleData) result.rule().getRuleData();

        assertThat(data).isNotNull();
        assertThat(data.isDeclarativeSituation()).isTrue();
    }

    @Test
    @DisplayName("Contains declarative situation and tax should validate")
    void should_validate_declarative_situation_and_tax() {
        Document document = documentWithAnalysis(List.of(fakeAvisDeclaratif(), fakeAvisImposition()));
        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_BAD_CLASSIFICATION);
    }

    @Test
    @DisplayName("Tax Assessment validation should pass")
    void should_validate_tax_assessment() {
        Document document = documentWithAnalysis(List.of(fakeAvisImposition()));
        RuleValidatorOutput result = rule.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_BAD_CLASSIFICATION);
    }

    @Test
    @DisplayName("Invalid document should fail")
    void should_fail_invalid_document() {
        // Document with empty barcodes or wrong type
        BarcodeModel barcode = BarcodeModel.builder()
                .antsType("other_type")
                .build();
        DocumentIAFileAnalysis analysis = fileAnalysisWithBarcode(barcode);

        RuleValidatorOutput result = rule.validate(documentWithAnalysis(List.of(analysis)));
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_BAD_CLASSIFICATION);

        var data = (TaxClassificationRuleData) result.rule().getRuleData();

        assertThat(data).isNotNull();
        assertThat(data.isDeclarativeSituation()).isFalse();
    }

    @Test
    @DisplayName("No analysis should be inconclusive")
    void should_be_inconclusive_when_no_analysis() {
        Document document = Document.builder().files(Collections.emptyList()).build();
        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_BAD_CLASSIFICATION);
    }

    // ==========================================
    // Fixtures
    // ==========================================

    private Document documentWithAnalysis(List<DocumentIAFileAnalysis> analysiss) {
        List<File> files = analysiss.stream()
                .map(analysis -> File.builder()
                        .documentIAFileAnalysis(analysis)
                        .build())
                .toList();
        return Document.builder()
                .files(files)
                .build();
    }

    private DocumentIAFileAnalysis fileAnalysisWithBarcode(BarcodeModel barcode) {
        ResultModel result = ResultModel.builder()
                .barcodes(List.of(barcode))
                .build();

        return DocumentIAFileAnalysis.builder()
                .documentIaExecutionId("exec-id")
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private DocumentIAFileAnalysis fakeAvisDeclaratif() {
        Map<String, Object> fields = Map.of(
                "41", "55956",
                "43", "2",
                "44", "25C2504151378",
                "45", "2024",
                "46", "JANE DOE", // Obfuscated
                "47", "3004669673404",
                "4B", "02062025",
                "4V", "6441",
                "4X", "5654",
                "4Y", "1 RUE DE LA PAIX 75000 PARIS" // Obfuscated
        );

        Map<String, Object> rawData = Map.of(
                "fields", fields,
                "country", "FR",
                "doc_type", "27",
                "perimeter", "01"
        );

        BarcodeModel barcode = BarcodeModel.builder()
                .pageNumber(1)
                .type("2D_DOC")
                .isValid(true)
                .rawData(rawData)
                .antsType(null)
                .typedData(Collections.emptyList())
                .build();

        return fileAnalysisWithBarcode(barcode);
    }

    private DocumentIAFileAnalysis fakeAvisImposition() {
        List<GenericProperty> typedData = List.of(
                GenericProperty.builder().name("doc_type").value("28").type("string").build(),
                GenericProperty.builder().name("nombre_de_parts").value(1.0).type("number").build(),
                GenericProperty.builder().name("reference_avis").value("1234567890123").type("string").build(),
                GenericProperty.builder().name("annee_des_revenus").value(2024).type("number").build(),
                GenericProperty.builder().name("declarant_1").value("JOHN DOE").type("string").build(),
                GenericProperty.builder().name("revenu_fiscal_de_reference").value(27284).type("number").build(),
                GenericProperty.builder().name("declarant_1_numero_fiscal").value("0123456789123").type("string").build(),
                GenericProperty.builder().name("date_mise_en_recouvrement").value("2025-07-31").type("date").build(),
                GenericProperty.builder().name("impot_revenu_net").value(1436).type("number").build(),
                GenericProperty.builder().name("retenue_a_la_source").value(1712).type("number").build()
        );

        BarcodeModel barcode = BarcodeModel.builder()
                .pageNumber(1)
                .type("2D_DOC")
                .isValid(true)
                .rawData(null)
                .typedData(typedData)
                .antsType("avis_imposition")
                .build();

        return fileAnalysisWithBarcode(barcode);
    }
}
