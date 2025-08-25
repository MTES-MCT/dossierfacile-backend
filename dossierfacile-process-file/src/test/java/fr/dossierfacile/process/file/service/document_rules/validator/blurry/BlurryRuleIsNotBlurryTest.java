package fr.dossierfacile.process.file.service.document_rules.validator.blurry;

import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class BlurryRuleIsNotBlurryTest {

    private File buildFile(Boolean isBlank, Boolean isBlurry, Boolean isReadable) {
        if (isBlank == null && isBlurry == null && isReadable == null) { // simulate no analysis
            return File.builder().build();
        }
        File f = File.builder().build();
        BlurryFileAnalysis analysis = BlurryFileAnalysis.builder()
                .blurryResults(new BlurryResult(isBlank, isBlurry, 42f, isReadable))
                .file(f)
                .build();
        f.setBlurryFileAnalysis(analysis);
        return f;
    }

    private Document buildDocument(File... files) {
        return Document.builder().files(Arrays.asList(files)).build();
    }

    @Test
    void pass_when_all_files_blank() {
        Document doc = buildDocument(
                buildFile(true, false, true),
                buildFile(true, true, true) // even if blurry flag true, skipped because blank
        );
        BlurryRuleIsNotBlurry rule = new BlurryRuleIsNotBlurry();
        RuleValidatorOutput out = rule.validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void pass_when_files_not_blank_and_not_blurry() {
        Document doc = buildDocument(
                buildFile(false, false, true), // readable & not blurry
                buildFile(true, true, true) // blank => skipped
        );
        BlurryRuleIsNotBlurry rule = new BlurryRuleIsNotBlurry();
        RuleValidatorOutput out = rule.validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    void fail_when_one_file_is_blurry() {
        Document doc = buildDocument(
                buildFile(false, false, true),
                buildFile(false, true, true) // readable & blurry
        );
        BlurryRuleIsNotBlurry rule = new BlurryRuleIsNotBlurry();
        RuleValidatorOutput out = rule.validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    void fail_when_one_file_unreadable() {
        Document doc = buildDocument(
                buildFile(false, false, true),
                buildFile(false, false, false) // unreadable => treated as blurry
        );
        BlurryRuleIsNotBlurry rule = new BlurryRuleIsNotBlurry();
        RuleValidatorOutput out = rule.validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    void pass_when_file_has_no_analysis() {
        Document doc = buildDocument(
                buildFile(null, null, null) // no analysis object
        );
        BlurryRuleIsNotBlurry rule = new BlurryRuleIsNotBlurry();
        RuleValidatorOutput out = rule.validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }
}

