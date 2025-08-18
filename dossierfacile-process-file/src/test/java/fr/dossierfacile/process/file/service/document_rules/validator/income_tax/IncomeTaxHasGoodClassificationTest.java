package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class IncomeTaxHasGoodClassificationTest {

    private File fileWithBarcode(BarCodeDocumentType type) {
        File f = File.builder().build();
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .documentType(type)
                .file(f)
                .build();
        f.setFileAnalysis(bar);
        return f;
    }

    private File fileWithoutBarcode() {
        return File.builder().build();
    }

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxHasGoodClassification().validate(d);
    }

    @Test
    @DisplayName("Passe si au moins un fichier TAX_ASSESSMENT présent avant tout break")
    void pass_with_tax_assessment() {
        Document doc = Document.builder()
                .files(List.of(
                        fileWithBarcode(BarCodeDocumentType.UNKNOWN),
                        fileWithBarcode(BarCodeDocumentType.TAX_ASSESSMENT)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_BAD_CLASSIFICATION);
    }

    @Test
    @DisplayName("Echec quand aucun fichier")
    void fail_no_files() {
        Document doc = Document.builder().build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec quand fichiers sans barcode")
    void fail_all_without_barcode() {
        Document doc = Document.builder()
                .files(List.of(fileWithoutBarcode(), fileWithoutBarcode()))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Echec quand barcodes présents mais aucun TAX_ASSESSMENT")
    void fail_wrong_types_only() {
        Document doc = Document.builder()
                .files(List.of(
                        fileWithBarcode(BarCodeDocumentType.UNKNOWN),
                        fileWithBarcode(BarCodeDocumentType.TAX_DECLARATION)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Echec à cause de break précoce (premier fichier sans barcode, second TAX_ASSESSMENT ignoré)")
    void fail_due_to_break_on_null_analysis() {
        Document doc = Document.builder()
                .files(List.of(
                        fileWithoutBarcode(), // provoque break dans la boucle
                        fileWithBarcode(BarCodeDocumentType.TAX_ASSESSMENT)))
                .build();
        RuleValidatorOutput out = validate(doc);
        // Montre le comportement actuel (peut-être à revoir fonctionnellement)
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

