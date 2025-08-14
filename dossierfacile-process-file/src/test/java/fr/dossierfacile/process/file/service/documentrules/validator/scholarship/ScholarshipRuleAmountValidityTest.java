package fr.dossierfacile.process.file.service.documentrules.validator.scholarship;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.ScholarshipFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ScholarshipRuleAmountValidityTest {

    private File scholarshipFile(int annualAmount) {
        ScholarshipFile sf = ScholarshipFile.builder()
                .annualAmount(annualAmount)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.SCHOLARSHIP)
                .parsedFile(sf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new ScholarshipRuleAmountValidity().validate(d);
    }

    @Test
    void pass_when_monthly_sum_matches_average() {
        // annual 10000 => monthlyAverage = 1000
        Document doc = Document.builder()
                .monthlySum(1000)
                .files(List.of(scholarshipFile(10000)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_SCHOLARSHIP_AMOUNT);
    }

    @Test
    void pass_when_difference_is_exactly_ten() {
        // average 1000, monthlySum 1010 => diff = 10 (threshold inclusive)
        Document doc = Document.builder()
                .monthlySum(1010)
                .files(List.of(scholarshipFile(10000)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void fail_when_difference_greater_than_ten() {
        // average 1000, monthlySum 1011 => diff = 11 > 10
        Document doc = Document.builder()
                .monthlySum(1011)
                .files(List.of(scholarshipFile(10000)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void fail_when_no_scholarship_file() {
        Document doc = Document.builder()
                .monthlySum(1000)
                .files(List.of())
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }
}

