package fr.dossierfacile.process.file.service.document_rules.validator.france_identite;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class FranceIdentiteHasBeenParsedBITest {

    private File fileValid(String status) {
        FranceIdentiteApiResult parsed = FranceIdentiteApiResult.builder()
                .status(status)
                .attributes(FranceIdentiteApiResultAttributes.builder()
                        .familyName("DUPONT")
                        .givenName("JEAN")
                        .validityDate("2030-01-01")
                        .birthDate("1990-01-01")
                        .birthPlace("PARIS")
                        .build())
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.FRANCE_IDENTITE)
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File fileWithStatus(String status) {
        return fileValid(status);
    }

    private File fileWithMissingAttribute(String missing) {
        FranceIdentiteApiResultAttributes attrs = FranceIdentiteApiResultAttributes.builder()
                .familyName("DUPONT")
                .givenName("JEAN")
                .validityDate("2030-01-01")
                .birthDate("1990-01-01")
                .birthPlace("PARIS")
                .build();
        switch (missing) {
            case "family" -> attrs.setFamilyName(null);
            case "given" -> attrs.setGivenName(null);
            case "validity" -> attrs.setValidityDate(null);
            default -> throw new IllegalArgumentException("Invalid attribute: " + missing);
        }
        FranceIdentiteApiResult parsed = FranceIdentiteApiResult.builder()
                .status("OK")
                .attributes(attrs)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.FAILED)
                .classification(ParsedFileClassification.FRANCE_IDENTITE)
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File fileWrongClassification() {
        FranceIdentiteApiResult parsed = FranceIdentiteApiResult.builder()
                .status("OK")
                .attributes(FranceIdentiteApiResultAttributes.builder()
                        .familyName("DUPONT")
                        .givenName("JEAN")
                        .validityDate("2030-01-01")
                        .build())
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.FAILED)
                .classification(ParsedFileClassification.PAYSLIP) // mauvaise classification
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File fileAnalysisFailed() {
        FranceIdentiteApiResult parsed = FranceIdentiteApiResult.builder()
                .status("OK")
                .attributes(FranceIdentiteApiResultAttributes.builder()
                        .familyName("DUPONT")
                        .givenName("JEAN")
                        .validityDate("2030-01-01")
                        .build())
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.FAILED)
                .classification(ParsedFileClassification.FRANCE_IDENTITE)
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private RuleValidatorOutput validate(Document d) {
        return new FranceIdentiteHasBeenParsedBI().validate(d);
    }

    @Test
    @DisplayName("Passe quand un fichier FRANCE_IDENTITE avec status OK et attributs présents (analysisStatus=COMPLETED)")
    void valid_file_passes() {
        Document doc = Document.builder().files(List.of(fileValid("OK"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_FRANCE_IDENTITE_STATUS);
    }

    @Test
    @DisplayName("Inconclus quand aucun fichier")
    void no_files_inconclusive() {
        Document doc = Document.builder().build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("Inconclus quand status = ERROR_FILE")
    void error_file_status_inconclusive() {
        Document doc = Document.builder().files(List.of(fileWithStatus("ERROR_FILE"))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("Inconclus quand classification incorrecte")
    void wrong_classification_inconclusive() {
        Document doc = Document.builder().files(List.of(fileWrongClassification())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Inconclus quand analysisStatus != COMPLETED")
    void analysis_not_failed_inconclusive() {
        Document doc = Document.builder().files(List.of(fileAnalysisFailed())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Inconclus quand attribut manquant (nom, prénom ou date validité)")
    void missing_attribute_inconclusive() {
        Document doc = Document.builder().files(List.of(
                fileWithMissingAttribute("family"),
                fileWithMissingAttribute("given"),
                fileWithMissingAttribute("validity")
        )).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

