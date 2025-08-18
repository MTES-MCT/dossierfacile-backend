package fr.dossierfacile.process.file.service.document_rules.validator.rental;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Arrays;

class RentalHasBeenParsedTest {

    private File fileWithParsed() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(RentalReceiptFile.builder()
                        .tenantFullName("Jean Dupont")
                        .ownerFullName("Proprio")
                        .period(YearMonth.now())
                        .amount(750.0)
                        .build())
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
        return new RentalHasBeenParsed().validate(document);
    }

    @Test
    void pass_when_all_files_parsed() {
        Document doc = Document.builder().files(Arrays.asList(fileWithParsed(), fileWithParsed())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_PARSED);
    }

    @Test
    void fail_inconclusive_when_one_file_missing_analysis() {
        Document doc = Document.builder().files(Arrays.asList(fileWithParsed(), fileWithoutParsedAnalysis())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_PARSED);
    }

    @Test
    void fail_inconclusive_when_one_file_has_null_parsed() {
        Document doc = Document.builder().files(Arrays.asList(fileWithParsed(), fileWithNullParsed())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_PARSED);
    }
}

