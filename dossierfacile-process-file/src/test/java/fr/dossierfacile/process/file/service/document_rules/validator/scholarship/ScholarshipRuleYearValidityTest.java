package fr.dossierfacile.process.file.service.document_rules.validator.scholarship;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class ScholarshipRuleYearValidityTest {

    private File scholarshipFile(int startYear, int endYear) {
        ScholarshipFile sf = ScholarshipFile.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .startYear(startYear)
                .endYear(endYear)
                .annualAmount(1000)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.SCHOLARSHIP)
                .parsedFile(sf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new ScholarshipRuleYearValidity().validate(d);
    }

    @Test
    void pass_or_fail_when_end_year_equals_current_year_depends_on_date_boundary() {
        int currentYear = LocalDate.now().getYear();
        Document doc = Document.builder()
                .files(List.of(scholarshipFile(currentYear - 1, currentYear)))
                .build();
        RuleValidatorOutput out = validate(doc);

        boolean beforeBoundary = LocalDate.now().isBefore(LocalDate.of(currentYear, 9, 15));
        if (beforeBoundary) {
            Assertions.assertThat(out.isValid()).as("Avant le 15/09 endYear == currentYear doit être valide").isTrue();
            Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        } else {
            Assertions.assertThat(out.isValid()).as("Après (ou le) 15/09 endYear == currentYear doit être invalide").isFalse();
            Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        }
    }

    @Test
    void pass_when_end_year_strictly_greater_than_current_year() {
        int currentYear = LocalDate.now().getYear();
        Document doc = Document.builder()
                .files(List.of(scholarshipFile(currentYear, currentYear + 1)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    void fail_when_end_year_less_than_current_year() {
        int currentYear = LocalDate.now().getYear();
        Document doc = Document.builder()
                .files(List.of(scholarshipFile(currentYear - 2, currentYear - 1)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void fail_when_no_scholarship_file_present() {
        Document doc = Document.builder()
                .files(List.of())
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }
}

