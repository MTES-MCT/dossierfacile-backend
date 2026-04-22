package fr.dossierfacile.document.analysis.rule.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.rule.PayslipClassificationRuleData;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.ClassificationModel;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayslipClassificationValidatorBTest {

    private final PayslipClassificationValidatorB rule = new PayslipClassificationValidatorB();

    @Test
    @DisplayName("PASSED quand tous les fichiers sont classifiés bulletin_salaire")
    void should_pass_when_all_files_are_payslip() {
        Document document = documentWithAnalyses(
                analysisWithClassification("bulletin_salaire"),
                analysisWithClassification("bulletin_salaire")
        );

        RuleValidatorOutput output = rule.validate(document);

        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(output.rule().getRule()).isEqualTo(DocumentRule.R_DOCUMENT_IA_CLASSIFICATION);
        assertThat(output.rule().getRuleData()).isInstanceOf(PayslipClassificationRuleData.class);
        PayslipClassificationRuleData data = (PayslipClassificationRuleData) output.rule().getRuleData();
        assertThat(data.entriesInError()).isEmpty();
    }

    @Test
    @DisplayName("FAILED quand un fichier n'est pas classifié bulletin_salaire")
    void should_fail_and_return_entries_in_error_when_one_file_is_not_payslip() {
        Document document = documentWithAnalyses(
                analysisWithClassification("bulletin_salaire"),
                analysisWithClassification("carte_identite")
        );

        RuleValidatorOutput output = rule.validate(document);

        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.rule().getRuleData()).isInstanceOf(PayslipClassificationRuleData.class);
        PayslipClassificationRuleData data = (PayslipClassificationRuleData) output.rule().getRuleData();
        assertThat(data.entriesInError()).hasSize(1);
        assertThat(data.entriesInError().get(0).fileId()).isEqualTo(2L);
        assertThat(data.entriesInError().get(0).fileName()).isEqualTo("file-2.pdf");
    }

    @Test
    @DisplayName("FAILED quand la classification est absente")
    void should_fail_when_classification_is_missing() {
        Document document = documentWithAnalyses(
                analysisWithClassification("bulletin_salaire"),
                analysisWithNoClassification()
        );

        RuleValidatorOutput output = rule.validate(document);

        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        PayslipClassificationRuleData data = (PayslipClassificationRuleData) output.rule().getRuleData();
        assertThat(data.entriesInError()).hasSize(1);
        assertThat(data.entriesInError().get(0).fileId()).isEqualTo(2L);
        assertThat(data.entriesInError().get(0).fileName()).isEqualTo("file-2.pdf");
    }

    @Test
    @DisplayName("FAILED quand aucune analyse n'est disponible")
    void should_fail_when_no_analysis() {
        Document document = Document.builder().files(List.of()).build();

        RuleValidatorOutput output = rule.validate(document);

        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.rule().getRuleData()).isInstanceOf(PayslipClassificationRuleData.class);
        PayslipClassificationRuleData data = (PayslipClassificationRuleData) output.rule().getRuleData();
        assertThat(data.entriesInError()).isEmpty();
    }

    private Document documentWithAnalyses(DocumentIAFileAnalysis... analyses) {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < analyses.length; i++) {
            DocumentIAFileAnalysis analysis = analyses[i];
            File file = File.builder()
                    .id((long) (i + 1))
                    .storageFile(StorageFile.builder().name("file-" + (i + 1) + ".pdf").build())
                    .documentIAFileAnalysis(analysis)
                    .build();
            if (analysis != null) {
                analysis.setFile(file);
            }
            files.add(file);
        }
        return Document.builder().files(files).build();
    }

    private DocumentIAFileAnalysis analysisWithClassification(String documentType) {
        ResultModel result = ResultModel.builder()
                .classification(ClassificationModel.builder().documentType(documentType).build())
                .build();
        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private DocumentIAFileAnalysis analysisWithNoClassification() {
        ResultModel result = ResultModel.builder()
                .classification(null)
                .build();
        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }
}
