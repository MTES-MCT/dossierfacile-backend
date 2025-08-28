package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class IncomeTaxRuleCheckNMinus1Test {

    private final ObjectMapper mapper = new ObjectMapper();

    private File taxQrFile(int year) {
        ObjectNode node = mapper.createObjectNode();
        // Champs minimum utilisés dans IncomeTaxHelper.fromQR (tous requis sinon NPE ou parse error)
        node.put(TwoDDocDataType.ID_47.getLabel(), "NF1");
        node.put(TwoDDocDataType.ID_46.getLabel(), "DOE");
        node.put(TwoDDocDataType.ID_49.getLabel(), "NF2");
        node.put(TwoDDocDataType.ID_48.getLabel(), "SMITH");
        node.put(TwoDDocDataType.ID_45.getLabel(), String.valueOf(year)); // anneeDesRevenus
        node.put(TwoDDocDataType.ID_43.getLabel(), "2");
        node.put(TwoDDocDataType.ID_4A.getLabel(), "20240101");
        node.put(TwoDDocDataType.ID_41.getLabel(), "12345");
        node.put(TwoDDocDataType.ID_44.getLabel(), "REF");
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .barCodeType(BarCodeType.TWO_D_DOC)
                .verifiedData(node)
                .build();
        File f = File.builder().fileAnalysis(bar).build();
        bar.setFile(f);
        return f;
    }

    private File nonQrFile() {
        // barCodeType différent -> ignoré par helper
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .barCodeType(BarCodeType.QR_CODE)
                .build();
        File f = File.builder().fileAnalysis(bar).build();
        bar.setFile(f);
        return f;
    }

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxRuleCheckNMinus1().validate(d);
    }

    @Test
    @DisplayName("Passe avec année N-1")
    void pass_with_year_n_minus_1() {
        int nowYear = LocalDate.now().getYear();
        Document doc = Document.builder().files(List.of(taxQrFile(nowYear - 1))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_N1);
    }

    @Test
    @DisplayName("Passe avec année N-2 avant le 15/09")
    void pass_with_year_n_minus_2_before_mid_september() {
        LocalDate now = LocalDate.now();
        Assumptions.assumeTrue(now.isBefore(LocalDate.of(now.getYear(), 9, 15)), "Test applicable seulement avant le 15/09");
        int nowYear = now.getYear();
        Document doc = Document.builder().files(List.of(taxQrFile(nowYear - 2))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec avec année non autorisée")
    void fail_with_disallowed_year() {
        LocalDate now = LocalDate.now();
        int nowYear = now.getYear();
        int invalidYear;
        if (now.isBefore(LocalDate.of(nowYear, 9, 15))) {
            invalidYear = nowYear - 3; // avant 15/09 N-3 refusé
        } else {
            invalidYear = nowYear - 2; // après 15/09 seul N-1 accepté
        }
        Document doc = Document.builder().files(List.of(taxQrFile(invalidYear))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec quand aucun QR 2D-Doc (liste années vide)")
    void fail_when_no_years() {
        Document doc = Document.builder().files(List.of(nonQrFile())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Echec même si N-1 présent mais année courante (N) aussi => maxYear invalide")
    void fail_when_current_year_present_with_valid_year() {
        int nowYear = LocalDate.now().getYear();
        // Ajouter N (courant) + N-1; maxYear = N => invalide
        Document doc = Document.builder().files(List.of(
                taxQrFile(nowYear),
                taxQrFile(nowYear - 1)
        )).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

