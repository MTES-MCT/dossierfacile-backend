package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.BlurryFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.ocr.BlurryResult;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

class BlurryRulesValidationServiceTest {

    private final BlurryRulesValidationService service = new BlurryRulesValidationService();

    private File fileWithStatusAndResult(BlurryFileAnalysisStatus status, boolean blank, boolean blurry) {
        File f = File.builder().build();
        BlurryFileAnalysis analysis = BlurryFileAnalysis.builder()
                .analysisStatus(status)
                .blurryResults(new BlurryResult(blank, blurry, Optional.of(10f), Optional.of(true), Optional.empty(), Optional.empty()))
                .file(f)
                .build();
        f.setBlurryFileAnalysis(analysis);
        return f;
    }

    private File fileWithoutAnalysis() {
        return File.builder().build();
    }

    private Document documentWithFiles(File... files) {
        return Document.builder().files(List.of(files)).build();
    }

    private DocumentAnalysisReport emptyReport(Document d) {
        return DocumentAnalysisReport.builder()
                .document(d)
                .failedRules(new LinkedList<>())
                .passedRules(new LinkedList<>())
                .inconclusiveRules(new LinkedList<>())
                .build();
    }

    @Test
    void shouldBeApplied_true_when_all_files_have_analysis() {
        Document doc = documentWithFiles(
                fileWithStatusAndResult(BlurryFileAnalysisStatus.COMPLETED, true, false),
                fileWithStatusAndResult(BlurryFileAnalysisStatus.FAILED, true, false)
        );
        Assertions.assertThat(service.shouldBeApplied(doc)).isTrue();
    }

    @Test
    void shouldBeApplied_false_when_one_file_missing_analysis() {
        Document doc = documentWithFiles(
                fileWithStatusAndResult(BlurryFileAnalysisStatus.COMPLETED, true, false),
                fileWithoutAnalysis()
        );
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void process_all_rules_pass_when_not_all_blank_and_not_blurry() {
        Document doc = documentWithFiles(
                fileWithStatusAndResult(BlurryFileAnalysisStatus.COMPLETED, false, false),
                fileWithStatusAndResult(BlurryFileAnalysisStatus.COMPLETED, true, false)
        );
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getPassedRules())
                .extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_BLURRY_FILE_ANALYSED,
                        DocumentRule.R_BLURRY_FILE_BLANK,
                        DocumentRule.R_BLURRY_FILE
                );
        Assertions.assertThat(report.getFailedRules()).isEmpty();
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    void process_stops_on_inconclusive_second_rule_when_all_blank() {
        Document doc = documentWithFiles(
                fileWithStatusAndResult(BlurryFileAnalysisStatus.COMPLETED, true, false),
                fileWithStatusAndResult(BlurryFileAnalysisStatus.COMPLETED, true, false)
        );
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getPassedRules())
                .extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_BLURRY_FILE_ANALYSED);
        Assertions.assertThat(report.getInconclusiveRules())
                .extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_BLURRY_FILE_BLANK);
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }

    @Test
    void process_inconclusive_first_rule_blocks_others() {
        // Analyse FAILED -> première règle INCONCLUSIVE et bloque
        Document doc = documentWithFiles(
                fileWithStatusAndResult(BlurryFileAnalysisStatus.FAILED, true, false)
        );
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getInconclusiveRules())
                .extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_BLURRY_FILE_ANALYSED);
        Assertions.assertThat(report.getPassedRules()).isEmpty();
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }

    @Test
    void process_second_rule_passes_then_third_checks_blurriness() {
        Document doc = documentWithFiles(
                fileWithStatusAndResult(BlurryFileAnalysisStatus.COMPLETED, false, true)
        );
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getPassedRules())
                .extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_BLURRY_FILE_ANALYSED,
                        DocumentRule.R_BLURRY_FILE_BLANK
                );

        Assertions.assertThat(report.getFailedRules())
                .extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_BLURRY_FILE);
    }
}
