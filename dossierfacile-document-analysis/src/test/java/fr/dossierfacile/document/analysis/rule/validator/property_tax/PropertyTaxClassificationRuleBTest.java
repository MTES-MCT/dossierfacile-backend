package fr.dossierfacile.document.analysis.rule.validator.property_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.ClassificationModel;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyTaxClassificationRuleBTest {

    private final PropertyTaxClassificationRuleB rule = new PropertyTaxClassificationRuleB();

    @Test
    @DisplayName("Should pass when at least one document has the correct type (taxe_fonciere)")
    void should_pass_when_at_least_one_file_is_taxe_fonciere() {
        Document document = documentWithAnalyses(
                fakeAnalysis("taxe_fonciere", DocumentIAFileAnalysisStatus.SUCCESS),
                fakeAnalysis("autre", DocumentIAFileAnalysisStatus.SUCCESS)
        );

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.isValid()).isTrue();
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(result.rule().getRule()).isEqualTo(DocumentRule.R_DOCUMENT_IA_CLASSIFICATION);
    }

    @Test
    @DisplayName("Should fail when all documents have a wrong type (e.g. autre, autre)")
    void should_fail_when_all_files_are_autre() {
        Document document = documentWithAnalyses(
                fakeAnalysis("autre", DocumentIAFileAnalysisStatus.SUCCESS),
                fakeAnalysis("autre", DocumentIAFileAnalysisStatus.SUCCESS)
        );

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.isValid()).isFalse();
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        assertThat(result.isBlocking()).isTrue();
    }

    @Test
    @DisplayName("Should fail when document list has no files or no successful analyses")
    void should_fail_when_no_successful_analyses() {
        Document document = documentWithAnalyses(
                fakeAnalysis("taxe_fonciere", DocumentIAFileAnalysisStatus.FAILED)
        );

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.isValid()).isFalse();
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("Should fail when classification is null")
    void should_fail_when_classification_is_null() {
        Document document = documentWithAnalyses(
                fakeAnalysis(null, DocumentIAFileAnalysisStatus.SUCCESS)
        );

        RuleValidatorOutput result = rule.validate(document);

        assertThat(result.isValid()).isFalse();
        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    private Document documentWithAnalyses(DocumentIAFileAnalysis... analyses) {
        List<File> files = Arrays.stream(analyses)
                .map(analysis -> File.builder().documentIAFileAnalysis(analysis).build())
                .toList();
        return Document.builder()
                .files(files)
                .build();
    }

    private DocumentIAFileAnalysis fakeAnalysis(String documentType, DocumentIAFileAnalysisStatus status) {
        ClassificationModel classification = documentType != null
                ? ClassificationModel.builder().documentType(documentType).build()
                : null;

        ResultModel result = ResultModel.builder()
                .classification(classification)
                .build();

        return DocumentIAFileAnalysis.builder()
                .documentIaExecutionId("exec-id")
                .analysisStatus(status)
                .result(result)
                .build();
    }
}
