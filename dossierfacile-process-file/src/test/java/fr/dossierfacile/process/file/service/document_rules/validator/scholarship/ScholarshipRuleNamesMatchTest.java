package fr.dossierfacile.process.file.service.document_rules.validator.scholarship;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ScholarshipRuleNamesMatchTest {

    private File scholarshipFile(String firstName, String lastName) {
        ScholarshipFile sf = ScholarshipFile.builder()
                .firstName(firstName)
                .lastName(lastName)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.SCHOLARSHIP)
                .parsedFile(sf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private File nonScholarshipFile() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.PAYSLIP)
                .parsedFile(null)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new ScholarshipRuleNamesMatch().validate(d);
    }

    @Test
    void pass_when_names_match_with_lastname() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").preferredName("JDUP").build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(scholarshipFile("Jean", "Dupont")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    void pass_when_names_match_with_preferred_name() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Durand").preferredName("Dupont").build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(scholarshipFile("Jean", "Dupont")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void fail_when_first_name_mismatch() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(scholarshipFile("Pierre", "Dupont")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void fail_when_last_and_preferred_names_mismatch() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Martin").preferredName("JPR").build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(scholarshipFile("Jean", "Dupont")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    void fail_when_no_scholarship_file_present() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder()
                .tenant(tenant)
                .files(List.of(nonScholarshipFile()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    void pass_with_guarantor_when_tenant_null() {
        Guarantor guarantor = Guarantor.builder().firstName("Alice").lastName("Leroy").build();
        Document doc = Document.builder()
                .guarantor(guarantor)
                .files(List.of(scholarshipFile("Alice", "Leroy")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void fail_with_guarantor_name_mismatch() {
        Guarantor guarantor = Guarantor.builder().firstName("Alice").lastName("Leroy").build();
        Document doc = Document.builder()
                .guarantor(guarantor)
                .files(List.of(scholarshipFile("Alice", "Martin")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

