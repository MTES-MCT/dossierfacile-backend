package fr.dossierfacile.process.file.service.documentrules.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class PayslipRuleMonthValidityTest {

    private File payslip(YearMonth ym) {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(PayslipFile.builder()
                        .fullname("TEST USER")
                        .month(ym)
                        .netTaxableIncome(1000.0)
                        .cumulativeNetTaxableIncome(3000.0)
                        .build())
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new PayslipRuleMonthValidity().validate(d);
    }

    private List<YearMonth> expectedComboPrimary() {
        LocalDate today = LocalDate.now();
        YearMonth now = YearMonth.now();
        if (today.getDayOfMonth() <= 15) {
            return List.of(now.minusMonths(1), now.minusMonths(2), now.minusMonths(3));
        } else {
            return List.of(now, now.minusMonths(1), now.minusMonths(2));
        }
    }

    private List<YearMonth> expectedComboSecondary() {
        LocalDate today = LocalDate.now();
        YearMonth now = YearMonth.now();
        if (today.getDayOfMonth() <= 15) {
            return List.of(now.minusMonths(2), now.minusMonths(3), now.minusMonths(4));
        } else {
            return List.of(now.minusMonths(1), now.minusMonths(2), now.minusMonths(3));
        }
    }

    @Test
    @DisplayName("Passe avec la première combinaison attendue exacte")
    void pass_with_primary_combo() {
        List<YearMonth> combo = expectedComboPrimary();
        Document doc = Document.builder()
                .files(combo.stream().map(this::payslip).toList())
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_MONTHS);
    }

    @Test
    @DisplayName("Passe avec la seconde combinaison attendue exacte")
    void pass_with_secondary_combo() {
        List<YearMonth> combo = expectedComboSecondary();
        Document doc = Document.builder()
                .files(combo.stream().map(this::payslip).toList())
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Passe avec un superset contenant la combinaison attendue")
    void pass_with_superset() {
        List<YearMonth> combo = expectedComboPrimary();
        // Ajouter un mois supplémentaire qui ne casse pas la règle (containsAll)
        List<File> files = new ArrayList<>(combo.stream().map(this::payslip).toList());
        files.add(payslip(YearMonth.now().minusMonths(12))); // ancien mois arbitraire
        Document doc = Document.builder().files(files).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec si aucune des combinaisons n'est couverte")
    void fail_when_no_combo_matches() {
        YearMonth now = YearMonth.now();
        // Choisir des mois lointains pour ne pas couvrir combos (ex: -6,-7,-8)
        List<File> files = List.of(
                payslip(now.minusMonths(6)),
                payslip(now.minusMonths(7)),
                payslip(now.minusMonths(8))
        );
        Document doc = Document.builder().files(files).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }
}

