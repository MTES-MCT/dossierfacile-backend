package fr.dossierfacile.process.file.service.document_rules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

class FranceIdentiteNumeriqueRulesValidationServiceTest {

    private final FranceIdentiteNumeriqueRulesValidationService service = new FranceIdentiteNumeriqueRulesValidationService();

    private File fiValidFile(String given, String family, String status) {
        FranceIdentiteApiResult parsed = FranceIdentiteApiResult.builder()
                .status(status)
                .attributes(FranceIdentiteApiResultAttributes.builder()
                        .givenName(given)
                        .familyName(family)
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

    private File fiInvalidForParsing() {
        // classification FRANCE_IDENTITE mais parsedFile null => helper renvoie empty -> première règle INCONCLUSIVE
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.FAILED)
                .classification(ParsedFileClassification.FRANCE_IDENTITE)
                .parsedFile(null)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
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
    @DisplayName("shouldBeApplied vrai si sous-catégorie FRANCE_IDENTITE")
    void shouldBeApplied_true() {
        Document doc = Document.builder().documentSubCategory(DocumentSubCategory.FRANCE_IDENTITE).build();
        Assertions.assertThat(service.shouldBeApplied(doc)).isTrue();
    }

    @Test
    @DisplayName("shouldBeApplied faux si autre sous-catégorie")
    void shouldBeApplied_false() {
        Document doc = Document.builder().documentSubCategory(DocumentSubCategory.FRENCH_PASSPORT).build();
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    @DisplayName("Process - les deux règles passent")
    void process_all_rules_pass() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder()
                .tenant(tenant)
                .documentSubCategory(DocumentSubCategory.FRANCE_IDENTITE)
                .files(List.of(fiValidFile("JEAN", "DUPONT", "OK")))
                .build();
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getFailedRules()).isEmpty();
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_FRANCE_IDENTITE_STATUS,
                        DocumentRule.R_FRANCE_IDENTITE_NAMES
                );
    }

    @Test
    @DisplayName("Process - première règle inconclusive (bloquante) stoppe")
    void process_blocks_on_first_inconclusive() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        Document doc = Document.builder()
                .tenant(tenant)
                .documentSubCategory(DocumentSubCategory.FRANCE_IDENTITE)
                .files(List.of(fiInvalidForParsing()))
                .build();
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getInconclusiveRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_FRANCE_IDENTITE_STATUS);
        Assertions.assertThat(report.getPassedRules()).isEmpty();
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }

    @Test
    @DisplayName("Process - noms KO après statut OK")
    void process_names_fail_after_status_pass() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        // parsed file avec noms différents => deuxième règle échoue
        Document doc = Document.builder()
                .tenant(tenant)
                .documentSubCategory(DocumentSubCategory.FRANCE_IDENTITE)
                .files(List.of(fiValidFile("PAUL", "MARTIN", "OK")))
                .build();
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_FRANCE_IDENTITE_STATUS);
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_FRANCE_IDENTITE_NAMES);
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }
}

