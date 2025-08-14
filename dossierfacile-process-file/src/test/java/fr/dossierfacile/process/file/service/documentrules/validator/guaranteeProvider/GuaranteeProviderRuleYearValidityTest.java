package fr.dossierfacile.process.file.service.documentrules.validator.guaranteeProvider;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.enums.ParsedStatus;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GuaranteeProviderRuleYearValidityTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private File fileWithValidity(String date, ParsedStatus status) {
        GuaranteeProviderFile gpf = GuaranteeProviderFile.builder()
                .validityDate(date)
                .status(status)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.GUARANTEE_PROVIDER)
                .parsedFile(gpf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private File fileWithoutParsed() { // provoque Optional.empty()
        return File.builder().build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new GuaranteeProviderRuleYearValidity().validate(d);
    }

    @Test
    void pass_when_validity_future_and_status_complete() {
        LocalDate future = LocalDate.now().plusDays(5);
        Document doc = Document.builder()
                .files(List.of(fileWithValidity(future.format(FMT), ParsedStatus.COMPLETE)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_GUARANTEE_EXPIRED);
    }

    @Test
    void fail_when_no_parsed_file() {
        Document doc = Document.builder()
                .files(List.of(fileWithoutParsed()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void fail_when_status_incomplete_even_if_future() {
        LocalDate future = LocalDate.now().plusDays(10);
        Document doc = Document.builder()
                .files(List.of(fileWithValidity(future.format(FMT), ParsedStatus.INCOMPLETE)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void fail_when_validity_today() {
        LocalDate today = LocalDate.now();
        Document doc = Document.builder()
                .files(List.of(fileWithValidity(today.format(FMT), ParsedStatus.COMPLETE)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse(); // isAfter(today) == false
    }

    @Test
    void fail_when_validity_past() {
        LocalDate past = LocalDate.now().minusDays(1);
        Document doc = Document.builder()
                .files(List.of(fileWithValidity(past.format(FMT), ParsedStatus.COMPLETE)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

