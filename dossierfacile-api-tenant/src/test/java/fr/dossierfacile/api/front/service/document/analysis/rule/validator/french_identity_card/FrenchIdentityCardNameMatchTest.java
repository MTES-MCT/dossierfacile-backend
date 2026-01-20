package fr.dossierfacile.api.front.service.document.analysis.rule.validator.french_identity_card;

import fr.dossierfacile.api.front.service.document.analysis.rule.validator.RuleValidatorOutput;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.documentIA.BarcodeModel;
import fr.dossierfacile.common.model.documentIA.ExtractionModel;
import fr.dossierfacile.common.model.documentIA.GenericProperty;
import fr.dossierfacile.common.model.documentIA.ResultModel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class FrenchIdentityCardNameMatchTest {

    private final FrenchIdentityCardNameMatch rule = new FrenchIdentityCardNameMatch();

    // ==========================
    // Helpers / Fixtures
    // ==========================

    private DocumentIAFileAnalysis iaAnalysisWithExtraction(
            String lastName,
            String firstName
    ) {
        ExtractionModel extraction = ExtractionModel.builder()
                .type("cni")
                .properties(Stream.of(
                        lastName != null ? GenericProperty.builder()
                                .name("nom")
                                .type("string")
                                .value(lastName)
                                .build() : null,
                        firstName != null ? GenericProperty.builder()
                                .name("prenom")
                                .type("string")
                                .value(firstName)
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
            String lastName,
            String usageName,
            String firstName,
            String listFirstNames
    ) {
        // On construit un seul barcode avec des typedData (GenericProperty)
        BarcodeModel barcode = BarcodeModel.builder()
                .type("DATA_MATRIX")
                .typedData(Stream.of(
                        lastName != null ? GenericProperty.builder()
                                .name("nom_patronymique")
                                .type("string")
                                .value(lastName)
                                .build() : null,
                        usageName != null ? GenericProperty.builder()
                                .name("nom_usage")
                                .type("string")
                                .value(usageName)
                                .build() : null,
                        firstName != null ? GenericProperty.builder()
                                .name("prenom")
                                .type("string")
                                .value(firstName)
                                .build() : null,
                        listFirstNames != null ? GenericProperty.builder()
                                .name("liste_prenoms")
                                .type("string")
                                .value(listFirstNames)
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

    private Document documentWithIaAnalysesAndTenant(String tenantFirstName, String tenantLastName, String preferredName, DocumentIAFileAnalysis... analyses) {
        Tenant tenant = Tenant.builder()
                .firstName(tenantFirstName)
                .lastName(tenantLastName)
                .preferredName(preferredName)
                .build();

        List<File> files = Stream.of(analyses)
                .map(analysis -> {
                    File file = File.builder()
                            .documentIAFileAnalysis(analysis)
                            .build();
                    analysis.setFile(file);
                    return file;
                })
                .toList();

        Document doc = Document.builder()
                .files(files)
                .tenant(tenant)
                .build();

        files.forEach(file -> file.setDocument(doc));

        return doc;
    }

    private Document documentWithIaAnalysesAndGuarantor(String guarantorFirstName, String guarantorLastName, DocumentIAFileAnalysis... analyses) {
        Guarantor guarantor = Guarantor.builder()
                .firstName(guarantorFirstName)
                .lastName(guarantorLastName)
                .build();

        List<File> files = Stream.of(analyses)
                .map(analysis -> {
                    File file = File.builder()
                            .documentIAFileAnalysis(analysis)
                            .build();
                    analysis.setFile(file);
                    return file;
                })
                .toList();

        Document doc = Document.builder()
                .files(files)
                .guarantor(guarantor)
                .build();

        files.forEach(file -> file.setDocument(doc));

        return doc;
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
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .build();

        Document doc = Document.builder()
                .files(List.of())
                .tenant(tenant)
                .build();

        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_FRENCH_IDENTITY_CARD_NAME_MATCH);
    }

    @Test
    @DisplayName("INCONCLUSIVE si le document n'a ni tenant ni guarantor")
    void inconclusive_when_no_tenant_no_guarantor() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction("Dupont", "Jean");

        List<File> files = List.of(File.builder()
                .documentIAFileAnalysis(analysis)
                .build());

        Document doc = Document.builder()
                .files(files)
                .tenant(null)
                .guarantor(null)
                .build();

        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("PASSED quand les noms correspondent exactement (2D-Doc, nom patronymique)")
    void passed_when_exact_match_2d_doc_patronymic() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "Dupont",  // nom_patronymique
                null,      // nom_usage
                "Jean",    // prenoms
                null       // liste_prenoms
        );

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED quand les noms correspondent avec accents normalisés (2D-Doc)")
    void passed_when_match_with_accents_2d_doc() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "Dupont",
                null,
                "François",
                null
        );

        Document doc = documentWithIaAnalysesAndTenant("Francois", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec nom d'usage qui matche (tenant a preferredName)")
    void passed_when_usage_name_matches_preferred_name() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "Dupont",  // nom_patronymique
                "Martin",  // nom_usage
                "Marie",
                null
        );

        Document doc = documentWithIaAnalysesAndTenant("Marie", "Dupont", "Martin", analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec liste de prénoms séparés par slash")
    void passed_when_multiple_first_names_slash_separated() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "Dupont",
                null,
                null,
                "Jean/Pierre/Paul"  // liste_prenoms
        );

        Document doc = documentWithIaAnalysesAndTenant("Pierre", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec prénoms séparés par espace dans extraction")
    void passed_when_first_names_space_separated_extraction() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(
                "Sagon",
                "Nicolas Patrick"  // prenom avec plusieurs prénoms
        );

        Document doc = documentWithIaAnalysesAndTenant("Nicolas", "Sagon", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED pour un guarantor avec correspondance exacte")
    void passed_when_guarantor_exact_match() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "Martin",
                null,
                "Sophie",
                null
        );

        Document doc = documentWithIaAnalysesAndGuarantor("Sophie", "Martin", analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @ParameterizedTest(name = "FAILED case {index}: {2}")
    @MethodSource("failedNameMatchCases")
    @DisplayName("FAILED quand le prénom et/ou le nom ne correspondent pas")
    void failed_when_name_or_first_name_do_not_match(
            String tenantFirstName,
            String tenantLastName,
            String description
    ) {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "Dupont",
                null,
                "Jean",
                null
        );

        Document doc = documentWithIaAnalysesAndTenant(tenantFirstName, tenantLastName, null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    private static Stream<Arguments> failedNameMatchCases() {
        return Stream.of(
                Arguments.of("Pierre", "Dupont", "Prénom différent"),
                Arguments.of("Jean", "Martin", "Nom de famille différent"),
                Arguments.of("Pierre", "Martin", "Prénom et nom différents")
        );
    }

    @Test
    @DisplayName("INCONCLUSIVE quand aucun nom trouvé dans le 2D-Doc")
    void inconclusive_when_no_name_in_2d_doc() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                null,  // pas de nom_patronymique
                null,  // pas de nom_usage
                "Jean",
                null
        );

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("INCONCLUSIVE quand aucun prénom trouvé dans le 2D-Doc")
    void inconclusive_when_no_first_name_in_2d_doc() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "Dupont",
                null,
                null,  // pas de prenoms
                null   // pas de liste_prenoms
        );

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("Utilise l'extraction si le 2D-Doc est incomplet")
    void use_extraction_when_2d_doc_incomplete() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(
                "Dupont",
                "Jean Pierre"
        );

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("INCONCLUSIVE quand l'extraction ne contient pas de nom")
    void inconclusive_when_extraction_missing_last_name() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(
                null,  // pas de nom
                "Jean"
        );

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("INCONCLUSIVE quand l'extraction ne contient pas de prénom")
    void inconclusive_when_extraction_missing_first_name() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(
                "Dupont",
                null  // pas de prenom
        );

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("PASSED avec noms composés et tirets")
    void passed_with_composite_names_and_hyphens() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "De La Cruz",
                null,
                "Jean-Pierre",
                null
        );

        Document doc = documentWithIaAnalysesAndTenant("JeanPierre", "DeLaCruz", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec casse différente")
    void passed_with_different_case() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "DUPONT",
                null,
                "JEAN",
                null
        );

        Document doc = documentWithIaAnalysesAndTenant("jean", "dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec caractères spéciaux normalisés")
    void passed_with_special_characters_normalized() {
        DocumentIAFileAnalysis analysis = iaAnalysisWith2DDoc(
                "Müller",
                null,
                "François",
                null
        );

        Document doc = documentWithIaAnalysesAndTenant("Francois", "Muller", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }
}

