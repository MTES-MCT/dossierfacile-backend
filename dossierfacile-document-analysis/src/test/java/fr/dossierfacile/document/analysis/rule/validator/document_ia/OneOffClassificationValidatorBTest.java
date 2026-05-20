package fr.dossierfacile.document.analysis.rule.validator.document_ia;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OneOffClassificationValidatorBTest {

    private final OneOffClassificationValidatorB validator =
            new OneOffClassificationValidatorB(List.of("quittance", "attestation_hebergement"));

    @Test
    @DisplayName("PASSED quand au moins une analyse SUCCESS a un type autorise")
    void should_pass_when_one_successful_analysis_has_an_authorized_type() {
        Document document = documentWithFiles(
                fileWithAnalysis(successAnalysis("carte_identite")),
                fileWithAnalysis(successAnalysis("quittance"))
        );

        RuleValidatorOutput output = validator.validate(document);

        assertThat(output.isValid()).isTrue();
        assertThat(output.isBlocking()).isTrue();
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(output.rule().getRule()).isEqualTo(DocumentRule.R_DOCUMENT_IA_CLASSIFICATION);
    }

    @Test
    @DisplayName("FAILED quand aucune analyse SUCCESS n'est disponible")
    void should_fail_when_no_successful_analysis_exists() {
        Document document = documentWithFiles(
                fileWithAnalysis(analysisWithStatus(DocumentIAFileAnalysisStatus.FAILED)),
                fileWithAnalysis(analysisWithStatus(DocumentIAFileAnalysisStatus.STARTED)),
                fileWithAnalysis(null)
        );

        RuleValidatorOutput output = validator.validate(document);

        assertThat(output.isValid()).isFalse();
        assertThat(output.isBlocking()).isTrue();
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.rule().getRule()).isEqualTo(DocumentRule.R_DOCUMENT_IA_CLASSIFICATION);
    }

    @Test
    @DisplayName("FAILED quand les analyses SUCCESS existent mais aucun type ne matche")
    void should_fail_when_successful_analyses_do_not_match_allowed_types() {
        Document document = documentWithFiles(
                fileWithAnalysis(successAnalysis("carte_identite")),
                fileWithAnalysis(successAnalysis("taxe_habitation"))
        );

        RuleValidatorOutput output = validator.validate(document);

        assertThat(output.isValid()).isFalse();
        assertThat(output.isBlocking()).isTrue();
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.rule().getRule()).isEqualTo(DocumentRule.R_DOCUMENT_IA_CLASSIFICATION);
    }

    private static Document documentWithFiles(File... files) {
        return Document.builder().files(List.of(files)).build();
    }

    private static File fileWithAnalysis(DocumentIAFileAnalysis analysis) {
        return File.builder().documentIAFileAnalysis(analysis).build();
    }

    private static DocumentIAFileAnalysis successAnalysis(String documentType) {
        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(ResultModel.builder()
                        .classification(ClassificationModel.builder().documentType(documentType).build())
                        .build())
                .build();
    }

    private static DocumentIAFileAnalysis analysisWithStatus(DocumentIAFileAnalysisStatus status) {
        return DocumentIAFileAnalysis.builder()
                .analysisStatus(status)
                .result(ResultModel.builder().build())
                .build();
    }
}

