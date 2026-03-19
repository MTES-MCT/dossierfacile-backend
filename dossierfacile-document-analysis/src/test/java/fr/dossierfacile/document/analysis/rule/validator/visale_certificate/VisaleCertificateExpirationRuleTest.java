package fr.dossierfacile.document.analysis.rule.validator.visale_certificate;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.rule.ExpirationRuleData;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class VisaleCertificateExpirationRuleTest {

    private final VisaleCertificateExpirationRule rule = new VisaleCertificateExpirationRule();

    // ==========================
    // Helpers / Fixtures
    // ==========================

    private DocumentIAFileAnalysis iaAnalysisWithExtraction(String dateFinValidite) {
        List<GenericProperty> properties = new ArrayList<>();

        properties.add(GenericProperty.builder()
                .name("numero_visa")
                .type("string")
                .value("V123456789")
                .build());

        properties.add(GenericProperty.builder()
                .name("date_delivrance")
                .type("date")
                .value("2026-03-02")
                .build());

        if (dateFinValidite != null) {
            properties.add(GenericProperty.builder()
                    .name("date_fin_validite")
                    .type("date")
                    .value(dateFinValidite)
                    .build());
        }

        ExtractionModel extraction = ExtractionModel.builder()
                .type("visale_certificate")
                .properties(properties)
                .build();

        ResultModel result = ResultModel.builder()
                .extraction(extraction)
                .barcodes(List.of())
                .build();

        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private Document documentWithIaAnalyses(DocumentIAFileAnalysis... analyses) {
        List<File> files = Stream.of(analyses)
                .map(analysis -> {
                    File file = File.builder()
                            .documentIAFileAnalysis(analysis)
                            .build();
                    analysis.setFile(file);
                    return file;
                })
                .toList();

        return Document.builder()
                .files(files)
                .build();
    }

    private RuleValidatorOutput validate(Document document) {
        return rule.validate(document);
    }

    // ==========================
    // Tests
    // ==========================

    @Test
    @DisplayName("INCONCLUSIVE si aucune analyse DocumentIA disponible")
    void inconclusive_when_no_analysis() {
        Document doc = Document.builder().files(List.of()).build();
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_VISALE_CERTIFICATE_EXPIRATION);
    }

    @Test
    @DisplayName("INCONCLUSIVE si pas de date de fin de validité")
    void inconclusive_when_no_expiration_date() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(null);

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("PASSED quand la date de fin de validité est dans le futur")
    void passed_when_expiration_in_future() {
        LocalDate future = LocalDate.now().plusMonths(3);
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(future.toString());

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);

        // Verify the rule data contains the expiration date
        Assertions.assertThat(out.rule().getRuleData()).isInstanceOf(ExpirationRuleData.class);
        ExpirationRuleData ruleData = (ExpirationRuleData) out.rule().getRuleData();
        Assertions.assertThat(ruleData.extractedDate()).isEqualTo(future);
    }

    @Test
    @DisplayName("PASSED quand la date de fin de validité est aujourd'hui")
    void passed_when_expiration_is_today() {
        LocalDate today = LocalDate.now();
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(today.toString());

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("FAILED quand la date de fin de validité est dans le passé")
    void failed_when_expiration_in_past() {
        LocalDate past = LocalDate.now().minusDays(1);
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(past.toString());

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);

        // Verify the rule data contains the expiration date
        Assertions.assertThat(out.rule().getRuleData()).isInstanceOf(ExpirationRuleData.class);
        ExpirationRuleData ruleData = (ExpirationRuleData) out.rule().getRuleData();
        Assertions.assertThat(ruleData.extractedDate()).isEqualTo(past);
    }

    @Test
    @DisplayName("FAILED quand le certificat est expiré depuis longtemps")
    void failed_when_expired_long_ago() {
        LocalDate longAgo = LocalDate.now().minusYears(1);
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(longAgo.toString());

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("PASSED quand le certificat expire dans longtemps")
    void passed_when_expires_far_in_future() {
        LocalDate farFuture = LocalDate.now().plusYears(1);
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(farFuture.toString());

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }
}
