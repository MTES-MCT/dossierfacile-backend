package fr.dossierfacile.process.file.service.document_rules.validator.rental;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RentalRuleMonthValidityTest {

    private File buildFile(YearMonth period) {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(RentalReceiptFile.builder().period(period).build())
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private List<List<YearMonth>> expectedTenantCombos() {
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.now();
        if (now.getDayOfMonth() <= 15) {
            return List.of(
                    List.of(ym.minusMonths(1), ym.minusMonths(2), ym.minusMonths(3)),
                    List.of(ym.minusMonths(2), ym.minusMonths(3), ym.minusMonths(4)),
                    List.of(ym.minusMonths(3), ym.minusMonths(4), ym.minusMonths(5))
            );
        } else {
            return List.of(
                    List.of(ym.minusMonths(1), ym.minusMonths(2), ym.minusMonths(3)),
                    List.of(ym.minusMonths(2), ym.minusMonths(3), ym.minusMonths(4))
            );
        }
    }

    private List<List<YearMonth>> expectedGuarantorCombos() {
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.now();
        if (now.getDayOfMonth() <= 15) {
            return List.of(
                    List.of(ym.minusMonths(1)),
                    List.of(ym.minusMonths(2)),
                    List.of(ym.minusMonths(3))
            );
        } else {
            return List.of(
                    List.of(ym.minusMonths(1)),
                    List.of(ym.minusMonths(2))
            );
        }
    }

    @Test
    void tenant_pass_when_has_one_full_expected_sequence() {
        List<YearMonth> combo = expectedTenantCombos().getFirst();
        Document doc = Document.builder()
                .tenant(Tenant.builder().firstName("A").lastName("B").build())
                .files(combo.stream().map(this::buildFile).toList())
                .build();
        RuleValidatorOutput out = new RentalRuleMonthValidity().validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_MONTHS);
    }

    @Test
    void tenant_fail_when_months_not_matching_any_sequence() {
        YearMonth ym = YearMonth.now();
        // Non consécutifs: m-1, m-3, m-5 ne correspond à aucune des listes attendues
        List<YearMonth> months = Arrays.asList(ym.minusMonths(1), ym.minusMonths(3), ym.minusMonths(5));
        Document doc = Document.builder()
                .tenant(fr.dossierfacile.common.entity.Tenant.builder().firstName("A").lastName("B").build())
                .files(months.stream().map(this::buildFile).toList())
                .build();
        RuleValidatorOutput out = new RentalRuleMonthValidity().validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void guarantor_pass_when_single_expected_month_present() {
        YearMonth target = expectedGuarantorCombos().getFirst().getFirst();
        Document doc = Document.builder()
                .files(List.of(buildFile(target))) // pas de tenant => mode guarantor
                .build();
        RuleValidatorOutput out = new RentalRuleMonthValidity().validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    void guarantor_fail_when_unexpected_month() {
        YearMonth ym = YearMonth.now();
        // Utilise le mois courant (jamais dans les listes attendues qui commencent à m-1)
        Document doc = Document.builder()
                .files(List.of(buildFile(ym)))
                .build();
        RuleValidatorOutput out = new RentalRuleMonthValidity().validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    void tenant_fail_when_partial_overlap_only() {
        // Prend deux mois consécutifs, mais pas le troisième attendu
        List<List<YearMonth>> combos = expectedTenantCombos();
        List<YearMonth> firstCombo = combos.get(0);
        List<YearMonth> partial = new ArrayList<>(firstCombo.subList(0, 2));
        Document doc = Document.builder()
                .tenant(fr.dossierfacile.common.entity.Tenant.builder().firstName("A").lastName("B").build())
                .files(partial.stream().map(this::buildFile).toList())
                .build();
        RuleValidatorOutput out = new RentalRuleMonthValidity().validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }
}

