package fr.dossierfacile.process.file.service.documentrules.validator.incomeTax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

public class IncomeTaxRuleCheckNMinus3Test {

    private final ObjectMapper mapper = new ObjectMapper();

    private File taxQrFile(int year) {
        ObjectNode node = mapper.createObjectNode();
        node.put(TwoDDocDataType.ID_47.getLabel(), "NF1");
        node.put(TwoDDocDataType.ID_46.getLabel(), "DOE");
        node.put(TwoDDocDataType.ID_49.getLabel(), "NF2");
        node.put(TwoDDocDataType.ID_48.getLabel(), "SMITH");
        node.put(TwoDDocDataType.ID_45.getLabel(), String.valueOf(year));
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

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxRuleCheckNMinus3().validate(d);
    }

    @Test
    @DisplayName("Passe quand l'année minimale est >= (authorisedYear - 2)")
    void pass_min_year_ok() {
        LocalDate now = LocalDate.now();
        int authorisedYear = now.minusMonths(9).minusDays(15).getYear();
        int allowedMin = authorisedYear - 2; // limite basse
        // fournir deux années dont la plus petite = allowedMin
        Document doc = Document.builder()
                .files(List.of(taxQrFile(authorisedYear), taxQrFile(allowedMin)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_N3);
    }

    @Test
    @DisplayName("Echec quand la liste des années est vide")
    void fail_no_years() {
        Document doc = Document.builder().build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec quand l'année minimale est < (authorisedYear - 2)")
    void fail_min_year_too_old() {
        LocalDate now = LocalDate.now();
        int authorisedYear = now.minusMonths(9).minusDays(15).getYear();
        int tooOld = (authorisedYear - 2) - 1; // juste en dessous de la limite
        Document doc = Document.builder()
                .files(List.of(taxQrFile(authorisedYear), taxQrFile(tooOld)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }
}

