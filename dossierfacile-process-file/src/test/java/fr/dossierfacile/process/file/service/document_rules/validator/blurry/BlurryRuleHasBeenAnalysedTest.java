package fr.dossierfacile.process.file.service.document_rules.validator.blurry;

import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class BlurryRuleHasBeenAnalysedTest {

    private File buildFileWithStatus(BlurryFileAnalysisStatus status) {
        File f = File.builder().build();
        BlurryFileAnalysis analysis = BlurryFileAnalysis.builder()
                .analysisStatus(status)
                .file(f)
                .build();
        f.setBlurryFileAnalysis(analysis);
        return f;
    }

    private File buildFileWithoutAnalysis() {
        return File.builder().build();
    }

    private Document buildDocument(List<File> files) {
        return Document.builder().files(files).build();
    }

    @Test
    void should_be_valid_when_all_files_analysed() {
        Document document = buildDocument(Arrays.asList(
                buildFileWithStatus(BlurryFileAnalysisStatus.COMPLETED),
                buildFileWithStatus(BlurryFileAnalysisStatus.COMPLETED)
        ));
        BlurryRuleHasBeenAnalysed rule = new BlurryRuleHasBeenAnalysed();

        RuleValidatorOutput out = rule.validate(document);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    void should_be_invalid_when_one_file_failed() {
        Document document = buildDocument(Arrays.asList(
                buildFileWithStatus(BlurryFileAnalysisStatus.COMPLETED),
                buildFileWithStatus(BlurryFileAnalysisStatus.FAILED)
        ));
        BlurryRuleHasBeenAnalysed rule = new BlurryRuleHasBeenAnalysed();

        RuleValidatorOutput out = rule.validate(document);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    void should_be_invalid_when_one_file_has_no_analysis() {
        Document document = buildDocument(Arrays.asList(
                buildFileWithStatus(BlurryFileAnalysisStatus.COMPLETED),
                buildFileWithoutAnalysis()
        ));
        BlurryRuleHasBeenAnalysed rule = new BlurryRuleHasBeenAnalysed();

        RuleValidatorOutput out = rule.validate(document);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }
}

