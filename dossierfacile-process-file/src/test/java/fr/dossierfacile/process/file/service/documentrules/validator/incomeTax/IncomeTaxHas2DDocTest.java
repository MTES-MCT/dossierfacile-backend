package fr.dossierfacile.process.file.service.documentrules.validator.incomeTax;

import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class IncomeTaxHas2DDocTest {

    private File fileWithBarcode() {
        File f = File.builder().build();
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder().file(f).build();
        f.setFileAnalysis(bar);
        return f;
    }

    private File fileWithoutBarcode() {
        return File.builder().build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxHas2DDoc().validate(d);
    }

    @Test
    @DisplayName("Passe si au moins un fichier possède une analyse 2D-Doc")
    void pass_when_at_least_one_barcode() {
        Document doc = Document.builder().files(List.of(fileWithoutBarcode(), fileWithBarcode())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_2D_DOC);
    }

    @Test
    @DisplayName("Inconclus quand aucun fichier dans le document")
    void inconclusive_when_no_files() {
        Document doc = Document.builder().build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_2D_DOC);
    }

    @Test
    @DisplayName("Inconclus quand fichiers sans analyse 2D-Doc")
    void inconclusive_when_no_barcode_files() {
        Document doc = Document.builder().files(List.of(fileWithoutBarcode(), fileWithoutBarcode())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_2D_DOC);
    }
}

