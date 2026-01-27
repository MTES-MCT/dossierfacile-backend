package fr.dossierfacile.document.analysis.rule.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PayslipContinuityRuleTest {

    // =========================================================================
    // FIXTURES
    // =========================================================================

    // Helper to create a validator fixed in time
    private PayslipContinuityRule createValidator(LocalDate currentFixedDate) {
        Clock fixedClock = Clock.fixed(
                currentFixedDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        return new PayslipContinuityRule(fixedClock);
    }

    private Document createDocumentWithAnalyses(DocumentIAFileAnalysis... analyses) {
        List<File> files = new ArrayList<>();
        for (DocumentIAFileAnalysis analysis : analyses) {
            File file = File.builder().build();
            file.setDocumentIAFileAnalysis(analysis);
            if (analysis != null) {
                analysis.setFile(file);
            }
            files.add(file);
        }
        return Document.builder().files(files).build();
    }

    // Note: On utilise "date_delivrance" car c'est ce qui est défini dans DocumentDate pour le moment.
    // Idéalement il faudrait mettre à jour DocumentDate pour utiliser un champ plus cohérent pour les fiches de paie.
    private DocumentIAFileAnalysis analysisWithDate(LocalDate startDate, LocalDate endDate) {
        ExtractionModel extraction = ExtractionModel.builder()
                .properties(List.of(
                        GenericProperty.builder()
                                .name("periode_debut")
                                .type("date")
                                .value(startDate.toString())
                                .build(),
                        GenericProperty.builder()
                                .name("periode_fin")
                                .type("date")
                                .value(endDate.toString())
                                .build()
                ))
                .build();

        ResultModel result = ResultModel.builder()
                .extraction(extraction)
                .barcodes(Collections.emptyList())
                .build();

        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(result)
                .build();
    }

    private DocumentIAFileAnalysis failedAnalysis() {
        return DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.FAILED)
                .build();
    }

    private static Stream<Arguments> provideContinuityScenarios() {
        return Stream.of(
                // Cas 1 : 10 Avril (<=15). Attend M-1 (Mars) .. M-4 (Dec).
                // Scénario : Fev, Jan, Dec -> OK (3 consécutifs dans la fenêtre)
                Arguments.of(
                        LocalDate.of(2023, 4, 10),
                        List.of(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 1, 1), LocalDate.of(2022, 12, 1)),
                        true
                ),
                // Cas 2 : 10 Avril (<=15).
                // Scénario : Mars, Fev, Jan -> OK (3 consécutifs dans la fenêtre)
                Arguments.of(
                        LocalDate.of(2023, 4, 10),
                        List.of(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 2, 1), LocalDate.of(2023, 1, 1)),
                        true
                ),
                // Cas 3 : 10 Avril (<=15).
                // Scénario : Mars, Fev, Jan, Dec -> OK (4 présents, donc 3 consécutifs ok)
                Arguments.of(
                        LocalDate.of(2023, 4, 10),
                        List.of(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 2, 1), LocalDate.of(2023, 1, 1), LocalDate.of(2022, 12, 1)),
                        true
                ),
                // Cas 4 : 10 Février 2024 (<=15). Changement d'année.
                // Fenêtre : Jan 24, Dec 23, Nov 23, Oct 23.
                // Scénario : Jan 24, Dec 23, Nov 23 -> OK
                Arguments.of(
                        LocalDate.of(2024, 2, 10),
                        List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2023, 12, 1), LocalDate.of(2023, 11, 1)),
                        true
                ),
                // Cas 5 : 10 Mai (<=15).
                // Fenêtre : Avril, Mars, Fev, Jan.
                // Scénario : Avril, Fev, Jan -> KO (Manque Mars pour faire suite)
                Arguments.of(
                        LocalDate.of(2023, 5, 10),
                        List.of(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 2, 1), LocalDate.of(2023, 1, 1)),
                        false
                ),
                // Cas 6 : 10 Avril (<=15).
                // Scénario : Mars, Mars, Jan -> KO (Doublon ne compte qu'une fois, donc Mars + Jan = pas de suite de 3)
                Arguments.of(
                        LocalDate.of(2023, 4, 10),
                        List.of(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, 1), LocalDate.of(2023, 1, 1)),
                        false
                ),
                // Cas 7 : 20 Avril (>15). Attend M (Avril) .. M-3 (Jan).
                // Scénario : Avril, Mars, Fev -> OK
                Arguments.of(
                        LocalDate.of(2023, 4, 20),
                        List.of(LocalDate.of(2023, 4, 1), LocalDate.of(2023, 3, 1), LocalDate.of(2023, 2, 1)),
                        true
                ),
                // Cas 8 : 20 Avril (>15).
                // Scénario : Mars, Fev, Jan -> OK
                Arguments.of(
                        LocalDate.of(2023, 4, 20),
                        List.of(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 2, 1), LocalDate.of(2023, 1, 1)),
                        true
                ),
                // Cas 9 : 20 Avril (>15).
                // Scénario : Fev, Jan, Dec -> KO (Dec n'est pas dans la fenêtre attendue [Avril..Jan])
                Arguments.of(
                        LocalDate.of(2023, 4, 20),
                        List.of(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 1, 1), LocalDate.of(2022, 12, 1)),
                        false
                )
        );
    }

    @ParameterizedTest(name = "Date: {0}, Mois fournis: {1} -> Valide: {2}")
    @MethodSource("provideContinuityScenarios")
    void checkContinuityRule(LocalDate currentDate, List<LocalDate> payslipDates, boolean expectedValidity) {
        PayslipContinuityRule validator = createValidator(currentDate);

        // Transforme la liste de dates en liste d'objets analysés
        DocumentIAFileAnalysis[] analyses = payslipDates.stream()
                .map(date -> analysisWithDate(date, date.plusMonths(1).minusDays(1)))
                .toArray(DocumentIAFileAnalysis[]::new);

        Document document = createDocumentWithAnalyses(analyses);

        RuleValidatorOutput result = validator.validate(document);

        assertThat(result.isValid()).isEqualTo(expectedValidity);
    }

    @Test
    @DisplayName("Devrait être INCONCLUSIVE si aucune analyse n'est disponible")
    void should_be_inconclusive_when_no_analysis() {
        PayslipContinuityRule validator = createValidator(LocalDate.of(2023, 4, 10));
        Document document = Document.builder().files(new ArrayList<>()).build();

        RuleValidatorOutput result = validator.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("Si une analyse est failed on statut que c'est inconclusive")
    void should_pass_when_mixed_with_failed_analyses() {
        LocalDate currentDate = LocalDate.of(2023, 4, 10);
        PayslipContinuityRule validator = createValidator(currentDate);

        // Analyse avec 3 mois consécutifs (valide) + 1 analyse échouée
        DocumentIAFileAnalysis[] analyses = new DocumentIAFileAnalysis[]{
                analysisWithDate(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, 31)),
                analysisWithDate(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28)),
                analysisWithDate(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31)),
                failedAnalysis()
        };

        Document document = createDocumentWithAnalyses(analyses);

        RuleValidatorOutput result = validator.validate(document);

        assertThat(result.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }
}
