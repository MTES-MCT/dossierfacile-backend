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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyTaxNamesRuleTest {

    private final PropertyTaxNamesRule rule = new PropertyTaxNamesRule();

    @Test
    @DisplayName("Should pass when the single owner matches the tenant (nominal)")
    void should_pass_when_single_owner_matches_tenant() {
        Document document = documentWithAnalysis(tenant("DUPONT", "Camille"), fakeTaxeFonciere(List.of("DUPONT Camille")));

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_PROPERTY_TAX_NAMES);
    }

    @Test
    @DisplayName("Should pass when identity is inverted")
    void should_pass_when_identity_inverted() {
        Document document = documentWithAnalysis(tenant("DUPONT", "Camille"), fakeTaxeFonciere(List.of("Camille DUPONT")));

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("Should pass when the tenant matches one of several owners")
    void should_pass_when_tenant_matches_one_of_several_owners() {
        Document document = documentWithAnalysis(
                tenant("DUPONT", "Marie"),
                fakeTaxeFonciere(List.of("DUPONT ANGELIQUE", "DUPONT MARIE"))
        );

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("Should fail when the tenant matches none of the owners")
    void should_fail_when_tenant_matches_none_of_the_owners() {
        Document document = documentWithAnalysis(
                tenant("MARTIN", "Jean"),
                fakeTaxeFonciere(List.of("DUPONT ANGELIQUE", "DUPONT MARIE"))
        );

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(result.isBlocking()).isTrue();
    }

    @Test
    @DisplayName("Should fail (refused) when no owner identity was extracted")
    void should_fail_when_owners_absent() {
        Document document = documentWithAnalysis(tenant("DUPONT", "Camille"), fakeTaxeFonciere(null));

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Should be inconclusive when the dossier has no identity")
    void should_be_inconclusive_when_dossier_has_no_identity() {
        Document document = Document.builder()
                .files(List.of(File.builder().documentIAFileAnalysis(fakeTaxeFonciere(List.of("DUPONT Camille"))).build()))
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

    private static DocumentIAFileAnalysis fakeTaxeFonciere(List<String> owners) {
        List<GenericProperty> properties = new java.util.ArrayList<>();
        properties.add(GenericProperty.builder().name("annee_imposition").value("2025").type("string").build());
        if (owners != null) {
            properties.add(GenericProperty.builder().name("identites_proprietaires").value(owners).type("list").build());
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
