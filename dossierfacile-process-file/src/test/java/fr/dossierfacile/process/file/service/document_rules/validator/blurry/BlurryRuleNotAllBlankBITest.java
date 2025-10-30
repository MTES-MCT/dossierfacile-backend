package fr.dossierfacile.process.file.service.document_rules.validator.blurry;

import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class BlurryRuleNotAllBlankBITest {

    private BlurryFileAnalysis buildBlurryFileAnalysis(boolean isBlank) {
        return BlurryFileAnalysis.builder()
                .blurryResults(new BlurryResult(isBlank, false, Optional.of(0f), Optional.of(true), Optional.empty(), Optional.empty()))
                .build();
    }

    private File buildFile(boolean isBlank) {
        File f = File.builder().build();
        BlurryFileAnalysis analysis = buildBlurryFileAnalysis(isBlank);
        analysis.setFile(f);
        f.setBlurryFileAnalysis(analysis);
        return f;
    }

    private Document buildDocument(boolean... blanks) {
        return Document.builder()
                .files(java.util.stream.IntStream.range(0, blanks.length)
                        .mapToObj(i -> buildFile(blanks[i]))
                        .toList())
                .build();
    }

    @Test
    void should_be_valid_when_at_least_one_file_not_blank() {
        Document document = buildDocument(true, false, true); // one file not blank
        BlurryRuleNotAllBlankBI rule = new BlurryRuleNotAllBlankBI();

        RuleValidatorOutput output = rule.validate(document);

        // Expect valid because all files are NOT blank (at least one not blank)
        Assertions.assertThat(output.isValid()).isTrue();
    }

    @Test
    void should_be_invalid_when_all_files_blank() {
        Document document = buildDocument(true, true); // all blank
        BlurryRuleNotAllBlankBI rule = new BlurryRuleNotAllBlankBI();

        RuleValidatorOutput output = rule.validate(document);

        // Expect invalid because all files are blank
        Assertions.assertThat(output.isValid()).isFalse();
    }
}
