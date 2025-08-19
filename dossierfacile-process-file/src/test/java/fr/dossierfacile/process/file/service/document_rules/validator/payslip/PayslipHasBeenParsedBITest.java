package fr.dossierfacile.process.file.service.document_rules.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

class PayslipHasBeenParsedBITest {

    private File fileWithParsed() {
        PayslipFile pf = PayslipFile.builder()
                .fullname("JEAN DUPONT")
                .month(YearMonth.now())
                .netTaxableIncome(2000.0)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(pf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private File fileWithoutParsedAnalysis() {
        return File.builder().build();
    }

    private File fileWithNullParsed() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(null)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document document) {
        return new PayslipHasBeenParsedBI().validate(document);
    }

    @Test
    void pass_when_all_files_parsed() {
        Document doc = Document.builder().files(List.of(fileWithParsed(), fileWithParsed())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_PARSING);
    }

    @Test
    void fail_inconclusive_when_one_file_missing_analysis() {
        Document doc = Document.builder().files(List.of(fileWithParsed(), fileWithoutParsedAnalysis())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_PARSING);
    }

    @Test
    void fail_inconclusive_when_one_file_has_null_parsed() {
        Document doc = Document.builder().files(List.of(fileWithParsed(), fileWithNullParsed())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_PARSING);
    }

    @Test
    void fail_inconclusive_when_no_files() {
        Document doc = Document.builder().files(List.of()).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_PARSING);
    }
}

