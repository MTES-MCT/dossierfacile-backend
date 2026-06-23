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
    @DisplayName("Should pass with year N-1 before Sept 15")
    void should_pass_with_previous_year_before_deadline() {
        // 2026-04-10 -> expected year 2025
        PropertyTaxYearRule validator = createValidator(LocalDate.of(2026, Month.APRIL, 10));

        RuleValidatorOutput result = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2025")));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_PROPERTY_TAX_WRONG_YEAR);
        TaxYearsRuleData data = (TaxYearsRuleData) result.rule().getRuleData();
        assertThat(data.expectedYear()).isEqualTo(2025);
        assertThat(data.extractedYears()).containsExactly(2025);
    }

    @Test
    @DisplayName("Should pass with year N from Sept 15")
    void should_pass_with_current_year_from_deadline() {
        // 2026-09-15 -> expected year 2026
        PropertyTaxYearRule validator = createValidator(LocalDate.of(2026, Month.SEPTEMBER, 15));

        RuleValidatorOutput result = validator.validate(documentWithAnalysis(fakeTaxeFonciere("2026")));

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
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
        properties.add(GenericProperty.builder().name("proprietaire_identite").value("DUPONT Camille").type("string").build());
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
