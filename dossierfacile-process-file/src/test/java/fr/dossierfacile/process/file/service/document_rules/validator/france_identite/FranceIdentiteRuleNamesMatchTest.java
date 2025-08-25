package fr.dossierfacile.process.file.service.document_rules.validator.france_identite;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class FranceIdentiteRuleNamesMatchTest {

    private File fiFile(String given, String family, String status) {
        FranceIdentiteApiResult parsed = FranceIdentiteApiResult.builder()
                .status(status)
                .attributes(FranceIdentiteApiResultAttributes.builder()
                        .givenName(given)
                        .familyName(family)
                        .validityDate("2030-01-01")
                        .birthDate("1990-01-01")
                        .birthPlace("PARIS")
                        .build())
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.FRANCE_IDENTITE)
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File fiWrongClassification() {
        FranceIdentiteApiResult parsed = FranceIdentiteApiResult.builder()
                .status("OK")
                .attributes(FranceIdentiteApiResultAttributes.builder()
                        .givenName("JEAN")
                        .familyName("DUPONT")
                        .validityDate("2030-01-01")
                        .build())
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.PAYSLIP)
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private RuleValidatorOutput validate(Document d) {
        return new FranceIdentiteRuleNamesMatch().validate(d);
    }

    @Test
    @DisplayName("Match simple exact")
    void match_exact() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder().tenant(tenant).files(List.of(fiFile("JEAN", "DUPONT", "OK"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_FRANCE_IDENTITE_NAMES);
    }

    @Test
    @DisplayName("Match avec second prénom dans l'API")
    void match_contains_extra_given_names() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder().tenant(tenant).files(List.of(fiFile("Jean Francois", "Dupont", "OK"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Match (casse et accents ignorés)")
    void match_case_insensitive() {
        Tenant tenant = Tenant.builder().firstName("jean").lastName("dûpônt").build();
        Document doc = Document.builder().tenant(tenant).files(List.of(fiFile("JEÁN", "DUPONT", "OK"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec si prénom ne correspond pas")
    void fail_first_name_mismatch() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder().tenant(tenant).files(List.of(fiFile("PAUL", "DUPONT", "OK"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec si nom ne correspond pas")
    void fail_last_name_mismatch() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder().tenant(tenant).files(List.of(fiFile("Jean", "MARTIN", "OK"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Echec si helper ne retourne rien (status ERROR_FILE)")
    void fail_error_file_status() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder().tenant(tenant).files(List.of(fiFile("Jean", "Dupont", "ERROR_FILE"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Echec classification différente")
    void fail_wrong_classification() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder().tenant(tenant).files(List.of(fiWrongClassification())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Guarantor utilisé si pas de tenant")
    void guarantor_used_when_no_tenant() {
        Guarantor guarantor = Guarantor.builder().firstName("Luc").lastName("Martin").build();
        Document doc = Document.builder().guarantor(guarantor).files(List.of(fiFile("Luc", "MARTIN", "OK"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }
}

