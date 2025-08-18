package fr.dossierfacile.process.file.service.document_rules.validator.guarantee_provider;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class GuaranteeProviderHasBeenParsedTest {

    private File fileWithGuaranteeProvider() {
        GuaranteeProviderFile gpf = GuaranteeProviderFile.builder()
                .visaNumber("VISA123")
                .signed(true)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.GUARANTEE_PROVIDER)
                .parsedFile(gpf)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private File fileWithoutParsed() {
        return File.builder().build();
    }

    private File fileWithNullParsed() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.GUARANTEE_PROVIDER)
                .parsedFile(null)
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new GuaranteeProviderHasBeenParsed().validate(d);
    }

    @Test
    void present_file_valid() {
        Document doc = Document.builder()
                .files(List.of(fileWithGuaranteeProvider()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_GUARANTEE_PARSING);
    }

    @Test
    void no_parsed_file_inconclusive() {
        Document doc = Document.builder()
                .files(List.of(fileWithoutParsed()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_GUARANTEE_PARSING);
    }

    @Test
    void null_parsed_file_inconclusive() {
        Document doc = Document.builder()
                .files(List.of(fileWithNullParsed()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_GUARANTEE_PARSING);
    }
}


