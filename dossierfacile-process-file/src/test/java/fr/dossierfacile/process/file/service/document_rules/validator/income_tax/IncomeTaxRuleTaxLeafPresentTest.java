package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class IncomeTaxRuleTaxLeafPresentTest {

    private File taxIncomeFileWithLeaves(int leafCount) {
        List<TaxIncomeLeaf> leaves = java.util.stream.IntStream.rangeClosed(1, leafCount)
                .mapToObj(i -> TaxIncomeLeaf.builder().page(i).pageCount(leafCount).build())
                .toList();
        TaxIncomeMainFile main = TaxIncomeMainFile.builder().taxIncomeLeaves(leaves).build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(main)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File taxIncomeFileEmptyLeaves() {
        TaxIncomeMainFile main = TaxIncomeMainFile.builder().taxIncomeLeaves(List.of()).build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(main)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File taxIncomeFileFailed() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.FAILED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(null)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File otherClassificationFile() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.PAYSLIP)
                .parsedFile(null)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File fileWithNullAnalysis() { return File.builder().build(); }

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxRuleTaxLeafPresent().validate(d);
    }

    @Test
    @DisplayName("Passe avec feuilles présentes")
    void pass_with_leaves_present() {
        Document doc = Document.builder().files(List.of(taxIncomeFileWithLeaves(3))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_LEAF);
    }

    @Test
    @DisplayName("Echec si analysis null")
    void fail_null_analysis() {
        Document doc = Document.builder().files(List.of(fileWithNullAnalysis())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec si status FAILED")
    void fail_failed_analysis() {
        Document doc = Document.builder().files(List.of(taxIncomeFileFailed())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Echec si feuilles vides")
    void fail_empty_leaves() {
        Document doc = Document.builder().files(List.of(taxIncomeFileEmptyLeaves())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Passe si uniquement autres classifications (règle ignore)")
    void pass_only_other_classification() {
        Document doc = Document.builder().files(List.of(otherClassificationFile())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Passe si aucun fichier (comportement actuel)")
    void pass_no_files() {
        Document doc = Document.builder().build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue(); // boucle vide -> true
    }

    @Test
    @DisplayName("Echec si mélange: un fichier KO suffit à faire échouer")
    void fail_when_one_ko() {
        Document doc = Document.builder().files(List.of(taxIncomeFileWithLeaves(2), taxIncomeFileEmptyLeaves())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

