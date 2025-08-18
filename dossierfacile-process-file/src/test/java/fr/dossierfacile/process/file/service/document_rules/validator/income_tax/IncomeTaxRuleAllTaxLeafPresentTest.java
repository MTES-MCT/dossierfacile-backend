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

class IncomeTaxRuleAllTaxLeafPresentTest {

    private File taxMainFile(int leafCount, Integer pageCount) {
        // construire la liste de feuilles
        List<TaxIncomeLeaf> leaves = java.util.stream.IntStream.rangeClosed(1, leafCount)
                .mapToObj(i -> TaxIncomeLeaf.builder().page(i).pageCount(pageCount).build())
                .toList();
        TaxIncomeMainFile main = TaxIncomeMainFile.builder()
                .taxIncomeLeaves(leaves)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(main)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File fileWithNullAnalysis() { return File.builder().build(); }

    private File fileWithFailedAnalysis() {
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

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxRuleAllTaxLeafPresent().validate(d);
    }

    @Test
    @DisplayName("Passe quand pageCount == nombre de feuilles")
    void pass_when_pagecount_equals_size() {
        Document doc = Document.builder()
                .files(List.of(taxMainFile(3, 3)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_LEAF);
    }

    @Test
    @DisplayName("Passe quand pageCount null (non vérifié)")
    void pass_when_pagecount_null() {
        Document doc = Document.builder()
                .files(List.of(taxMainFile(2, null)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec quand analyse null rencontrée")
    void fail_when_null_analysis() {
        Document doc = Document.builder()
                .files(List.of(fileWithNullAnalysis(), taxMainFile(2, 2)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec quand status FAILED")
    void fail_when_failed_analysis() {
        Document doc = Document.builder()
                .files(List.of(fileWithFailedAnalysis(), taxMainFile(2, 2)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Echec quand pageCount > nombre de feuilles")
    void fail_when_missing_leaf() {
        Document doc = Document.builder()
                .files(List.of(taxMainFile(2, 3)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Feuille d'autre classification ignorée")
    void pass_with_other_classification_present() {
        Document doc = Document.builder()
                .files(List.of(otherClassificationFile(), taxMainFile(1,1)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }
}

