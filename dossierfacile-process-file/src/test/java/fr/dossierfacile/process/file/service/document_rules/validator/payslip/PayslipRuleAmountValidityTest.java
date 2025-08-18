package fr.dossierfacile.process.file.service.document_rules.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

class PayslipRuleAmountValidityTest {

    private File payslip(YearMonth ym, double net) {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(PayslipFile.builder()
                        .fullname("TEST USER")
                        .month(ym)
                        .netTaxableIncome(net)
                        .cumulativeNetTaxableIncome(net) // non utilisé ici
                        .build())
                .build();
        return File.builder().parsedFileAnalysis(pfa).build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new PayslipRuleAmountValidity().validate(d);
    }

    @Test
    @DisplayName("OK diff < 20%")
    void pass_when_diff_lower_than_20_percent() {
        YearMonth now = YearMonth.now();
        Document doc = Document.builder()
                .monthlySum(2000)
                .files(List.of(
                        payslip(now, 2000),
                        payslip(now.minusMonths(1), 2100),
                        payslip(now.minusMonths(2), 1900)
                ))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_AMOUNT_MISMATCHES);
    }

    @Test
    @DisplayName("OK diff = 20% (borne incluse)")
    void pass_when_diff_equal_20_percent() {
        YearMonth now = YearMonth.now();
        // moyenne des 3 derniers = 2000
        Document doc = Document.builder()
                .monthlySum(2500) // diff = (2000-2500)/2500 = 0.2
                .files(List.of(
                        payslip(now, 2000),
                        payslip(now.minusMonths(1), 2100),
                        payslip(now.minusMonths(2), 1900)
                ))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName(">20% -> échec")
    void fail_when_diff_greater_than_20_percent() {
        YearMonth now = YearMonth.now();
        Document doc = Document.builder()
                .monthlySum(2501) // moyenne 2000 -> diff 501/2501 > 0.2
                .files(List.of(
                        payslip(now, 2000),
                        payslip(now.minusMonths(1), 2100),
                        payslip(now.minusMonths(2), 1900)
                ))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Seuls les 3 mois les plus récents sont pris en compte")
    void only_last_three_months_used() {
        YearMonth now = YearMonth.now();
        // 4 fiches, la plus ancienne (mois-3) a un montant aberrant qui ne doit pas influencer la moyenne
        Document doc = Document.builder()
                .monthlySum(3000)
                .files(List.of(
                        payslip(now, 3000),
                        payslip(now.minusMonths(1), 3000),
                        payslip(now.minusMonths(2), 3000),
                        payslip(now.minusMonths(3), 10000) // devrait être ignoré
                ))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Moins de 3 fiches : moyenne calculée sur présentes")
    void less_than_three_files() {
        YearMonth now = YearMonth.now();
        Document doc = Document.builder()
                .monthlySum(2050)
                .files(List.of(
                        payslip(now, 2000),
                        payslip(now.minusMonths(1), 2100)
                ))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Moins de 3 fiches : échec si diff >20%")
    void less_than_three_files_fail() {
        YearMonth now = YearMonth.now();
        Document doc = Document.builder()
                .monthlySum(1600) // moyenne 2050 -> diff 450/1600 = 28.1%
                .files(List.of(
                        payslip(now, 2000),
                        payslip(now.minusMonths(1), 2100)
                ))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

