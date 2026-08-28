package fr.dossierfacile.document.analysis.rule.validator.property_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.entity.rule.TaxYearsRuleData;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyTaxYearRuleTest {

    private PropertyTaxYearRule createValidator(LocalDate currentFixedDate) {
        Clock fixedClock = Clock.fixed(
                currentFixedDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        return new PropertyTaxYearRule(fixedClock);
    }

    @Test
    @DisplayName("Should pass only with year N-1 before Aug 1")
    void should_pass_only_with_previous_year_before_august() {
        // 2026-04-10 -> expectedYears: [2025], expectedYear in ruleData: 2025
        PropertyTaxYearRule validator = createValidator(LocalDate.of(2026, Month.APRIL, 10));

        // Year N-1 passes
        RuleValidatorOutput resultNMinus1 = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2025")));
        assertThat(resultNMinus1.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(resultNMinus1.rule().getRule()).isEqualTo(DocumentRule.R_PROPERTY_TAX_WRONG_YEAR);
        TaxYearsRuleData dataNMinus1 = (TaxYearsRuleData) resultNMinus1.rule().getRuleData();
        assertThat(dataNMinus1.expectedYear()).isEqualTo(2025);
        assertThat(dataNMinus1.extractedYears()).containsExactly(2025);

        // Year N fails (new tax notice not issued before Aug 1)
        RuleValidatorOutput resultN = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2026")));
        assertThat(resultN.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        TaxYearsRuleData dataN = (TaxYearsRuleData) resultN.rule().getRuleData();
        assertThat(dataN.expectedYear()).isEqualTo(2025);
        assertThat(dataN.extractedYears()).containsExactly(2026);
    }

    @Test
    @DisplayName("Should pass with year N-1 or N between Aug 1 and Oct 1")
    void should_pass_with_previous_or_current_year_between_august_and_october() {
        // 2026-08-15 -> expectedYears: [2025, 2026], expectedYear in ruleData: 2025
        PropertyTaxYearRule validator = createValidator(LocalDate.of(2026, Month.AUGUST, 15));

        // Year N-1 passes
        RuleValidatorOutput resultNMinus1 = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2025")));
        assertThat(resultNMinus1.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        TaxYearsRuleData dataNMinus1 = (TaxYearsRuleData) resultNMinus1.rule().getRuleData();
        assertThat(dataNMinus1.expectedYear()).isEqualTo(2025);
        assertThat(dataNMinus1.extractedYears()).containsExactly(2025);

        // Year N passes
        RuleValidatorOutput resultN = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2026")));
        assertThat(resultN.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        TaxYearsRuleData dataN = (TaxYearsRuleData) resultN.rule().getRuleData();
        assertThat(dataN.expectedYear()).isEqualTo(2025);
        assertThat(dataN.extractedYears()).containsExactly(2026);

        // Year N-2 fails
        RuleValidatorOutput resultNMinus2 = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2024")));
        assertThat(resultNMinus2.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Should pass only with year N from Oct 1")
    void should_pass_only_with_current_year_from_october() {
        // 2026-10-01 -> expectedYears: [2026], expectedYear in ruleData: 2026
        PropertyTaxYearRule validator = createValidator(LocalDate.of(2026, Month.OCTOBER, 1));

        // Year N passes
        RuleValidatorOutput resultN = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2026")));
        assertThat(resultN.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        TaxYearsRuleData dataN = (TaxYearsRuleData) resultN.rule().getRuleData();
        assertThat(dataN.expectedYear()).isEqualTo(2026);
        assertThat(dataN.extractedYears()).containsExactly(2026);

        // Year N-1 fails
        RuleValidatorOutput resultNMinus1 = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2025")));
        assertThat(resultNMinus1.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(resultNMinus1.isBlocking()).isTrue();
        TaxYearsRuleData dataNMinus1 = (TaxYearsRuleData) resultNMinus1.rule().getRuleData();
        assertThat(dataNMinus1.expectedYear()).isEqualTo(2026);
        assertThat(dataNMinus1.extractedYears()).containsExactly(2025);
    }

    @Test
    @DisplayName("Should fail with a wrong year (invalid)")
    void should_fail_with_wrong_year() {
        // 2026-04-10 -> expected year 2025
        PropertyTaxYearRule validator = createValidator(LocalDate.of(2026, Month.APRIL, 10));

        RuleValidatorOutput result = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2023")));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(result.isBlocking()).isTrue();
        TaxYearsRuleData data = (TaxYearsRuleData) result.rule().getRuleData();
        assertThat(data.expectedYear()).isEqualTo(2025);
        assertThat(data.extractedYears()).containsExactly(2023);
    }

    @Test
    @DisplayName("Should fail (refused) when the year is absent")
    void should_fail_when_year_absent() {
        PropertyTaxYearRule validator = createValidator(LocalDate.of(2026, Month.APRIL, 10));

        RuleValidatorOutput result = validator.validate(documentWithAnalysis(fakeTaxeFonciere(null)));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Should be inconclusive when the year is not parseable")
    void should_be_inconclusive_when_year_unparseable() {
        PropertyTaxYearRule validator = createValidator(LocalDate.of(2026, Month.APRIL, 10));

        RuleValidatorOutput result = validator.validate(documentWithAnalysis(fakeTaxeFonciere("inconnu")));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    // ==========================================
    // Fixtures
    // ==========================================

    private Document documentWithAnalysis(DocumentIAFileAnalysis analysis) {
        return Document.builder()
                .files(List.of(File.builder().documentIAFileAnalysis(analysis).build()))
                .build();
    }

    private DocumentIAFileAnalysis fakeTaxeFonciere(String anneeImposition) {
        List<GenericProperty> properties = new ArrayList<>();
        properties.add(GenericProperty.builder().name("identites_proprietaires").value(List.of("DUPONT Camille")).type("list").build());
        if (anneeImposition != null) {
            properties.add(GenericProperty.builder().name("annee_imposition").value(anneeImposition).type("string").build());
        }

        ResultModel result = ResultModel.builder()
                .extraction(ExtractionModel.builder().type("taxe_fonciere").properties(properties).build())
                .build();

        return DocumentIAFileAnalysis.builder()
                .documentIaExecutionId("exec-id")
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }
}
