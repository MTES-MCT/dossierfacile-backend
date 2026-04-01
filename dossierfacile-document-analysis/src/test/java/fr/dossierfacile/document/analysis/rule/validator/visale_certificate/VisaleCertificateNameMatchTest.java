package fr.dossierfacile.document.analysis.rule.validator.visale_certificate;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class VisaleCertificateNameMatchTest {

    private final VisaleCertificateNameMatch rule = new VisaleCertificateNameMatch();

    // ==========================
    // Helpers / Fixtures
    // ==========================

    private GenericProperty beneficiaireItem(String prenoms, String nom) {
        List<GenericProperty> beneficiaireProperties = new ArrayList<>();
        if (nom != null) {
            beneficiaireProperties.add(GenericProperty.builder()
                    .name("nom")
                    .type("string")
                    .value(nom)
                    .build());
        }
        if (prenoms != null) {
            beneficiaireProperties.add(GenericProperty.builder()
                    .name("prenoms")
                    .type("string")
                    .value(prenoms)
                    .build());
        }
        return GenericProperty.builder()
                .name("item")
                .type("object")
                .value(beneficiaireProperties)
                .build();
    }

    private DocumentIAFileAnalysis iaAnalysisWithExtraction(List<GenericProperty> beneficiaireItems) {
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

        properties.add(GenericProperty.builder()
                .name("date_fin_validite")
                .type("date")
                .value("2026-05-31")
                .build());

        if (!beneficiaireItems.isEmpty()) {
            properties.add(GenericProperty.builder()
                    .name("beneficiaires")
                    .type("list")
                    .value(beneficiaireItems)
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

    private Document documentWithIaAnalysesAndTenant(String tenantFirstName, String tenantLastName, String preferredName, DocumentIAFileAnalysis... analyses) {
        Tenant tenant = Tenant.builder()
                .id(1L)
                .firstName(tenantFirstName)
                .lastName(tenantLastName)
                .preferredName(preferredName)
                .build();

        Guarantor guarantor = Guarantor.builder()
                .id(10L)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .tenant(tenant)
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
                .tenant(null)
                .build();

        files.forEach(file -> file.setDocument(doc));

        return doc;
    }

    private Document documentWithIaAnalysesAndCouple(
            String tenant1FirstName,
            String tenant1LastName,
            String tenant2FirstName,
            String tenant2LastName,
            DocumentIAFileAnalysis... analyses
    ) {
        Tenant tenant1 = Tenant.builder()
                .id(1L)
                .firstName(tenant1FirstName)
                .lastName(tenant1LastName)
                .build();

        Tenant tenant2 = Tenant.builder()
                .id(2L)
                .firstName(tenant2FirstName)
                .lastName(tenant2LastName)
                .build();

        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .applicationType(ApplicationType.COUPLE)
                .tenants(List.of(tenant1, tenant2))
                .build();

        tenant1.setApartmentSharing(apartmentSharing);
        tenant2.setApartmentSharing(apartmentSharing);

        Guarantor guarantor = Guarantor.builder()
                .id(10L)
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .tenant(tenant1)
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
                .tenant(null)
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

        Guarantor guarantor = Guarantor.builder()
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .tenant(tenant)
                .build();

        Document doc = Document.builder()
                .files(List.of())
                .guarantor(guarantor)
                .tenant(null)
                .build();

        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_VISALE_CERTIFICATE_NAME_MATCH);
    }

    @Test
    @DisplayName("INCONCLUSIVE si le document n'a pas de tenant")
    void inconclusive_when_no_tenant() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Jean", "Dupont")
        ));

        Guarantor guarantor = Guarantor.builder()
                .typeGuarantor(TypeGuarantor.NATURAL_PERSON)
                .tenant(null)
                .build();

        List<File> files = List.of(File.builder()
                .documentIAFileAnalysis(analysis)
                .build());

        Document doc = Document.builder()
                .files(files)
                .guarantor(guarantor)
                .tenant(null)
                .build();

        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("INCONCLUSIVE si aucun bénéficiaire dans le certificat")
    void inconclusive_when_no_beneficiaires() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of());

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);

        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("PASSED quand le nom du tenant correspond exactement à un bénéficiaire")
    void passed_when_exact_match() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Jean", "Dupont")
        ));

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED quand le nom du tenant correspond avec normalisation (accents)")
    void passed_when_match_with_accents() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("François", "Müller")
        ));

        Document doc = documentWithIaAnalysesAndTenant("Francois", "Muller", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED quand le nom correspond parmi plusieurs bénéficiaires")
    void passed_when_match_among_multiple_beneficiaires() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Sophie", "Martin"),
                beneficiaireItem("Jean", "Dupont")
        ));

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED quand le co-tenant d'un couple correspond à un bénéficiaire")
    void passed_when_co_tenant_matches() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Jean", "Dupont"),
                beneficiaireItem("Sophie", "Martin")
        ));

        Document doc = documentWithIaAnalysesAndCouple("Jean", "Dupont", "Sophie", "Martin", analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED quand seulement le co-tenant correspond (pas le tenant principal)")
    void passed_when_only_co_tenant_matches() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Sophie", "Martin")
        ));

        Document doc = documentWithIaAnalysesAndCouple("Jean", "Dupont", "Sophie", "Martin", analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @ParameterizedTest(name = "FAILED case {index}: {2}")
    @MethodSource("failedNameMatchCases")
    @DisplayName("FAILED quand le nom du tenant ne correspond à aucun bénéficiaire")
    void failed_when_no_match(
            String tenantFirstName,
            String tenantLastName,
            String description
    ) {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Jean", "Dupont")
        ));

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
    @DisplayName("FAILED quand ni le tenant ni le co-tenant ne correspondent")
    void failed_when_no_match_in_couple() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Pierre", "Durand")
        ));

        Document doc = documentWithIaAnalysesAndCouple("Jean", "Dupont", "Sophie", "Martin", analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("PASSED avec casse différente")
    void passed_with_different_case() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("JEAN", "DUPONT")
        ));

        Document doc = documentWithIaAnalysesAndTenant("jean", "dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec noms composés et tirets")
    void passed_with_composite_names_and_hyphens() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Jean-Pierre", "De La Cruz")
        ));

        Document doc = documentWithIaAnalysesAndTenant("JeanPierre", "DeLaCruz", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec fallback fuzzy sur prénom")
    void passed_with_fuzzy_first_name_match() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Alandro", "Dupont")
        ));

        Document doc = documentWithIaAnalysesAndTenant("Aleandro", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec fallback fuzzy sur nom")
    void passed_with_fuzzy_last_name_match() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Jean", "Duppont")
        ));

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("FAILED quand fallback fuzzy dépasse la distance max")
    void failed_when_fuzzy_distance_too_high() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Alesxxndro", "Duxxont")
        ));

        Document doc = documentWithIaAnalysesAndTenant("Aleandro", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("FAILED avec noms courts quand le strict échoue (pas de fallback fuzzy)")
    void failed_when_short_names_and_strict_fails() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Li", "Xu")
        ));

        Document doc = documentWithIaAnalysesAndTenant("Lu", "Xi", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("PASSED avec fallback fuzzy sur co-tenant")
    void passed_with_fuzzy_match_on_co_tenant() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Sofie", "Martan")
        ));

        Document doc = documentWithIaAnalysesAndCouple("Jean", "Dupont", "Sophie", "Martin", analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("INCONCLUSIVE quand les bénéficiaires sont invalides (sans nom)")
    void inconclusive_when_beneficiaires_invalid() {
        DocumentIAFileAnalysis analysis = iaAnalysisWithExtraction(List.of(
                beneficiaireItem("Jean", null)
        ));

        Document doc = documentWithIaAnalysesAndTenant("Jean", "Dupont", null, analysis);
        RuleValidatorOutput out = validate(doc);

        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }
}
