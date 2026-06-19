package fr.dossierfacile.document.analysis.rule.validator.property_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyTaxNamesRuleTest {

    private final PropertyTaxNamesRule rule = new PropertyTaxNamesRule();

    @Test
    @DisplayName("Should pass when the owner identity matches the tenant (nominal)")
    void should_pass_when_owner_matches_tenant() {
        Document document = documentWithAnalysis(tenant("DUPONT", "Camille"), fakeTaxeFonciere("DUPONT Camille"));

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_PROPERTY_TAX_NAMES);
    }

    @Test
    @DisplayName("Should pass when identity is inverted")
    void should_pass_when_identity_inverted() {
        Document document = documentWithAnalysis(tenant("DUPONT", "Camille"), fakeTaxeFonciere("Camille DUPONT"));

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("Should fail when the owner identity does not match (invalid)")
    void should_fail_when_owner_does_not_match() {
        Document document = documentWithAnalysis(tenant("MARTIN", "Jean"), fakeTaxeFonciere("DUPONT Camille"));

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(result.isBlocking()).isTrue();
    }

    @Test
    @DisplayName("Should fail (refused) when the owner identity was not extracted")
    void should_fail_when_owner_identity_absent() {
        Document document = documentWithAnalysis(tenant("DUPONT", "Camille"), fakeTaxeFonciere(null));

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Should be inconclusive when the dossier has no identity")
    void should_be_inconclusive_when_dossier_has_no_identity() {
        Document document = Document.builder()
                .files(List.of(File.builder().documentIAFileAnalysis(fakeTaxeFonciere("DUPONT Camille")).build()))
                .build();

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    // ==========================================
    // Fixtures
    // ==========================================

    private static Tenant tenant(String lastName, String firstName) {
        return Tenant.builder().lastName(lastName).firstName(firstName).build();
    }

    private static Document documentWithAnalysis(Tenant tenant, DocumentIAFileAnalysis analysis) {
        return Document.builder()
                .tenant(tenant)
                .files(List.of(File.builder().documentIAFileAnalysis(analysis).build()))
                .build();
    }

    private static DocumentIAFileAnalysis fakeTaxeFonciere(String proprietaireIdentite) {
        List<GenericProperty> properties = new ArrayList<>();
        properties.add(GenericProperty.builder().name("annee_imposition").value("2025").type("string").build());
        if (proprietaireIdentite != null) {
            properties.add(GenericProperty.builder().name("proprietaire_identite").value(proprietaireIdentite).type("string").build());
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
