package fr.dossierfacile.process.file.service.document_rules.validator.payslip;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

class PayslipRuleNamesMatchTest {

    private File payslip(String fullname) {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(PayslipFile.builder()
                        .fullname(fullname)
                        .month(YearMonth.now())
                        .netTaxableIncome(2000.0)
                        .cumulativeNetTaxableIncome(6000.0)
                        .build())
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new PayslipRuleNamesMatch().validate(d);
    }

    @Test
    @DisplayName("Nom/prénom dans l'ordre Nom Prénom")
    void last_first_order_matches() {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(payslip("DUPONT JEAN")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_NAME);
    }

    @Test
    @DisplayName("Nom/prénom dans l'ordre Prénom Nom")
    void first_last_order_matches() {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(payslip("Jean Dupont")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Préfixes (Mme/M./etc) ignorés")
    void prefixes_removed() {
        Tenant tenant = Tenant.builder()
                .firstName("Sophie")
                .lastName("Durand")
                .build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(payslip("Mme Sophie Durand"), payslip("Mademoiselle Durand Sophie"), payslip("MR DURAND SOPHIE")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Nom préféré remplaçant le nom de famille")
    void preferred_name_as_last_name_matches() {
        // preferredName est utilisé comme alternative au lastName dans l'implémentation
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .preferredName("DURAND")
                .build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(payslip("Durand Jean")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec si ni nom ni nom préféré ne matchent")
    void fail_when_no_match() {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .preferredName("DURAND")
                .build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(payslip("Martin Paul")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Guarantor document : match sur garant")
    void guarantor_document_matches() {
        Guarantor guarantor = Guarantor.builder()
                .firstName("Luc")
                .lastName("Bernard")
                .build();
        Document doc = Document.builder()
                .guarantor(guarantor)
                .files(List.of(payslip("BERNARD LUC")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Plusieurs fichiers dont un mismatch -> échec")
    void multiple_files_one_mismatch() {
        Tenant tenant = Tenant.builder()
                .firstName("Emma")
                .lastName("Martin")
                .build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(payslip("Martin Emma"), payslip("Paul Dupont")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

