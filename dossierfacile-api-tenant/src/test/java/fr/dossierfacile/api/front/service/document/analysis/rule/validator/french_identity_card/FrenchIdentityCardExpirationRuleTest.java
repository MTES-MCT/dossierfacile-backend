package fr.dossierfacile.api.front.service.document.analysis.rule.validator.french_identity_card;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.documentIA.BarcodeModel;
import fr.dossierfacile.common.model.documentIA.ExtractionModel;
import fr.dossierfacile.common.model.documentIA.GenericProperty;
import fr.dossierfacile.common.model.documentIA.ResultModel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class FrenchIdentityCardExpirationRuleTest {

    private final FrenchIdentityCardExpirationRule rule = new FrenchIdentityCardExpirationRule();

    // ==========================
    // Helpers / Fixtures
    // ==========================

    private DocumentIAFileAnalysis iaAnalysisWithExtraction(
            String cardNumber,
            String birthDate,
            String deliveryDate,
            String expirationDate
    ) {
        ExtractionModel extraction = ExtractionModel.builder()
                .type("cni")
                .properties(Stream.of(
                        birthDate != null ? GenericProperty.builder()
                                .name("date_naissance")
                                .type("date")
                                .value(birthDate)
                                .build() : null,
                        deliveryDate != null ? GenericProperty.builder()
                                .name("date_delivrance")
                                .type("date")
                                .value(deliveryDate)
                                .build() : null,
                        expirationDate != null ? GenericProperty.builder()
                                .name("date_expiration")
                                .type("date")
                                .value(expirationDate)
                                .build() : null,
                        cardNumber != null ? GenericProperty.builder()
                                .name("numero_document")
                                .type("string")
                                .value(cardNumber)
                                .build() : null
                ).filter(Objects::nonNull).toList())
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

    private DocumentIAFileAnalysis iaAnalysisWith2DDoc(
            String cardNumber,
            String birthDate,
            String deliveryDate,
            String expirationDate
    ) {
        // On construit un seul barcode avec des typedData (GenericProperty)
        BarcodeModel barcode = BarcodeModel.builder()
                .type("DATA_MATRIX")
                .typedData(Stream.of(
                        birthDate != null ? GenericProperty.builder()
                                .name("date_naissance")
                                .type("date")
                                .value(birthDate)
                                .build() : null,
                        deliveryDate != null ? GenericProperty.builder()
                                .name("date_debut_validite")
                                .type("date")
                                .value(deliveryDate)
                                .build() : null,
                        expirationDate != null ? GenericProperty.builder()
                                .name("date_fin_validite")
                                .type("date")
                                .value(expirationDate)
                                .build() : null,
                        cardNumber != null ? GenericProperty.builder()
                                .name("numero_document")
                                .type("string")
                                .value(cardNumber)
                                .build() : null
                ).filter(Objects::nonNull).toList())
                .build();

        ResultModel result = ResultModel.builder()
                .barcodes(List.of(barcode))
                .extraction(null)
                .build();

        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private Document documentWithIaAnalyses(DocumentIAFileAnalysis... analyses) {
        // Créer un File pour chaque analyse (relation OneToOne)
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
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_FRENCH_IDENTITY_CARD_EXPIRATION);
    }

    @Test
    @DisplayName("PASSED quand la date d'expiration (2D-Doc) est dans le futur")
    void passed_when_expiration_in_future_from_2d_doc() {
        LocalDate future = LocalDate.now().plusYears(1);
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "123456789",
                "1990-01-01",
                future.minusYears(10).toString(),
                future.toString()
        );

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("FAILED quand la carte est expirée (nouvelle carte, numéro différent de 111)")
    void failed_when_expired_new_card() {
        LocalDate past = LocalDate.now().minusYears(1);
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "MCD123", // nouvelle carte
                "1990-01-01",
                past.minusYears(10).toString(),
                past.toString()
        );

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("INCONCLUSIVE quand la carte est expirée mais numéro 111 et données incomplètes")
    void inconclusive_when_old_card_111_but_missing_dates() {
        LocalDate past = LocalDate.now().minusYears(10);
        // On ne met que la date d'expiration, sans date de délivrance ni de naissance
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "111",
                null,
                null,
                past.toString()
        );

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("PASSED quand vieille CNI (numéro 111) reste valide après application de l'algorithme complexe")
    void passed_when_old_card_111_and_still_valid_after_complex_algorithm() {
        // Cas :
        // - Né il y a 40 ans
        // - Carte délivrée il y a 5 ans (majeur au moment de la délivrance)
        // - Expiration initiale il y a 1 an
        //   => réelle expiration = +5 ans => encore 4 ans de validité
        LocalDate birthDate = LocalDate.now().minusYears(40);
        LocalDate deliveryDate = LocalDate.now().minusYears(5);
        LocalDate expirationDate = LocalDate.now().minusYears(1);

        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "111",
                birthDate.toString(),
                deliveryDate.toString(),
                expirationDate.toString()
        );

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("FAILED quand vieille CNI (numéro 111) est expirée après algorithme complexe")
    void failed_when_old_card_111_and_expired_after_complex_algorithm() {
        // Cas :
        // - Né il y a 40 ans
        // - Carte délivrée il y a 15 ans (majeur au moment de la délivrance)
        // - Expiration initiale il y a 11 ans
        //   => réelle expiration = +5 ans => il y a 6 ans (donc expirée)
        LocalDate birthDate = LocalDate.now().minusYears(40);
        LocalDate deliveryDate = LocalDate.now().minusYears(15);
        LocalDate expirationDate = LocalDate.now().minusYears(11);

        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "111",
                birthDate.toString(),
                deliveryDate.toString(),
                expirationDate.toString()
        );

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Utilise l'extraction si le 2D-Doc ne contient pas de dates")
    void use_extraction_when_2d_doc_missing() {
        LocalDate future = LocalDate.now().plusYears(2);
        // Analyse sans barcodes (ou barcodes vides) mais avec extraction complète
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(
                "123456789",
                "1990-01-01",
                future.minusYears(10).toString(),
                future.toString()
        );

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("INCONCLUSIVE quand aucune date d'expiration trouvée ni en 2D-Doc ni en extraction")
    void inconclusive_when_no_expiration_anywhere() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(
                "123456789",
                "1990-01-01",
                null,
                null
        );

        Document doc = documentWithIaAnalyses(analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }
}
