package fr.dossierfacile.document.analysis.rule.validator.tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.entity.rule.TaxYearsRuleData;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaxYearRuleTest {

    private TaxYearRule createValidator(LocalDate currentFixedDate) {
        Clock fixedClock = Clock.fixed(
                currentFixedDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        return new TaxYearRule(fixedClock);
    }

    @Test
    @DisplayName("Should pass when tax year is correct (Before Sept 15)")
    void should_validate_correct_year_before_deadline() {
        LocalDate currentDate = LocalDate.of(2023, 4, 10);
        // Expected year: 2021
        TaxYearRule validator = createValidator(currentDate);

        Document document = documentWithAnalysis(List.of(fakeAvisImposition(2021)));

        RuleValidatorOutput result = validator.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_WRONG_YEAR);

        assertThat(result.rule().getRuleData()).isInstanceOf(TaxYearsRuleData.class);
        TaxYearsRuleData data = (TaxYearsRuleData) result.rule().getRuleData();
        assertThat(data.expectedYear()).isEqualTo(2021);
        assertThat(data.extractedYears()).containsExactly(2021);
    }

    @Test
    @DisplayName("Should fail when tax year is incorrect (Before Sept 15)")
    void should_fail_incorrect_year_before_deadline() {
        LocalDate currentDate = LocalDate.of(2023, 4, 10);
        // Expected year: 2021
        TaxYearRule validator = createValidator(currentDate);

        // Document has 2020
        Document document = documentWithAnalysis(List.of(fakeAvisImposition(2020)));

        RuleValidatorOutput result = validator.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_TAX_WRONG_YEAR);

        assertThat(result.rule().getRuleData()).isInstanceOf(TaxYearsRuleData.class);
        TaxYearsRuleData data = (TaxYearsRuleData) result.rule().getRuleData();
        assertThat(data.expectedYear()).isEqualTo(2021);
        assertThat(data.extractedYears()).containsExactly(2020);
    }

    @Test
    @DisplayName("Should pass when tax year is correct (After Sept 15)")
    void should_validate_correct_year_after_deadline() {
        LocalDate currentDate = LocalDate.of(2023, 9, 15);
        // Expected year: 2022
        TaxYearRule validator = createValidator(currentDate);

        Document document = documentWithAnalysis(List.of(fakeAvisImposition(2022)));

        RuleValidatorOutput result = validator.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);

        assertThat(result.rule().getRuleData()).isInstanceOf(TaxYearsRuleData.class);
        TaxYearsRuleData data = (TaxYearsRuleData) result.rule().getRuleData();
        assertThat(data.expectedYear()).isEqualTo(2022);
        assertThat(data.extractedYears()).containsExactly(2022);
    }

    @Test
    @DisplayName("Should fail when tax year is incorrect (After Sept 15)")
    void should_fail_incorrect_year_after_deadline() {
        LocalDate currentDate = LocalDate.of(2023, 9, 20);
        // Expected year: 2022
        TaxYearRule validator = createValidator(currentDate);

        // Document has 2021
        Document document = documentWithAnalysis(List.of(fakeAvisImposition(2021)));

        RuleValidatorOutput result = validator.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);

        assertThat(result.rule().getRuleData()).isInstanceOf(TaxYearsRuleData.class);
        TaxYearsRuleData data = (TaxYearsRuleData) result.rule().getRuleData();
        assertThat(data.expectedYear()).isEqualTo(2022);
        assertThat(data.extractedYears()).containsExactly(2021);
    }

    @Test
    @DisplayName("Should validate when multiple tax notices contain the correct year")
    void should_validate_multiple_tax_notices_with_one_valid_year() {
        LocalDate currentDate = LocalDate.of(2023, 4, 10);
        // Expected year: 2021
        TaxYearRule validator = createValidator(currentDate);

        // One document with correct year (2022), one with wrong year (2021)
        Document document = documentWithAnalysis(List.of(fakeAvisImposition(2021), fakeAvisImposition(2022)));

        RuleValidatorOutput result = validator.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);

        assertThat(result.rule().getRuleData()).isInstanceOf(TaxYearsRuleData.class);
        TaxYearsRuleData data = (TaxYearsRuleData) result.rule().getRuleData();
        assertThat(data.expectedYear()).isEqualTo(2021);
        assertThat(data.extractedYears()).containsExactlyInAnyOrder(2021, 2022);
    }

    @Test
    @DisplayName("Should be inconclusive when not tax document")
    void should_be_inconclusive_when_not_tax_document() {
        LocalDate currentDate = LocalDate.of(2023, 4, 10);
        // Expected year: 2021
        TaxYearRule validator = createValidator(currentDate);

        Document document = documentWithAnalysis(List.of(fakeOtherDocument()));

        RuleValidatorOutput result = validator.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);

        assertThat(result.rule().getRuleData()).isInstanceOf(TaxYearsRuleData.class);
        TaxYearsRuleData data = (TaxYearsRuleData) result.rule().getRuleData();
        assertThat(data.expectedYear()).isEqualTo(2021);
        assertThat(data.extractedYears()).isEmpty();
    }

    @Test
    @DisplayName("Should be inconclusive when year is missing")
    void should_be_inconclusive_when_year_missing() {
        LocalDate currentDate = LocalDate.of(2023, 4, 10);
        TaxYearRule validator = createValidator(currentDate);

        Document document = documentWithAnalysis(List.of(fakeAvisImpositionWithoutYear()));

        RuleValidatorOutput result = validator.validate(document);
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
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


    private DocumentIAFileAnalysis fakeOtherDocument() {
        BarcodeModel barcode = BarcodeModel.builder()
                .type("2D_DOC")
                .isValid(true)
                .antsType("other_type")
                .build();
        return fileAnalysisWithBarcode(barcode);
    }

    private DocumentIAFileAnalysis fakeAvisImpositionWithoutYear() {
        BarcodeModel barcode = BarcodeModel.builder()
                .type("2D_DOC")
                .isValid(true)
                .typedData(Collections.emptyList())
                .antsType("avis_imposition")
                .build();
        return fileAnalysisWithBarcode(barcode);
    }

    private DocumentIAFileAnalysis fakeAvisImposition(int year) {
        List<GenericProperty> typedData = List.of(
                GenericProperty.builder().name("doc_type").value("28").type("string").build(),
                GenericProperty.builder().name("nombre_de_parts").value(1.0).type("number").build(),
                GenericProperty.builder().name("reference_avis").value("1234567890123").type("string").build(),
                GenericProperty.builder().name("annee_des_revenus").value(year).type("number").build(),
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
