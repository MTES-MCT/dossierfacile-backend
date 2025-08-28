package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;
import java.util.List;

class IncomeTaxHasBeenParsedBITest {

    private File taxFile(Integer year, String declarant1Nom, Integer rfr) {
        TaxIncomeMainFile parsed = TaxIncomeMainFile.builder()
                .anneeDesRevenus(year)
                .declarant1Nom(declarant1Nom)
                .revenuFiscalDeReference(rfr)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File taxFileMissingField(String missing) {
        TaxIncomeMainFile.TaxIncomeMainFileBuilder b = TaxIncomeMainFile.builder()
                .anneeDesRevenus(2023)
                .declarant1Nom("DUPONT")
                .revenuFiscalDeReference(12345);
        switch (missing) {
            case "year" -> b.anneeDesRevenus(null);
            case "nom" -> b.declarant1Nom(null);
            case "rfr" -> b.revenuFiscalDeReference(null);
            default -> throw new IllegalArgumentException("Invalid field: " + missing);
        }
        TaxIncomeMainFile parsed = b.build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private File taxFileNullParsed() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(null)
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
        PayslipFile pf = PayslipFile.builder()
                .fullname("X Y")
                .month(YearMonth.now())
                .netTaxableIncome(1000.0)
                .cumulativeNetTaxableIncome(1000.0)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.PAYSLIP)
                .parsedFile(pf)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxHasBeenParsedBI().validate(d);
    }

    @Test
    @DisplayName("Passe quand un fichier TAX_INCOME avec les 3 champs requis est présent")
    void pass_valid_tax_file() {
        Document doc = Document.builder()
                .files(List.of(otherClassificationFile(), taxFile(2023, "DUPONT", 12345)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_PARSE);
    }

    @Test
    @DisplayName("Inconclus si aucun fichier TAX_INCOME valide")
    void inconclusive_no_valid_tax_file() {
        Document doc = Document.builder()
                .files(List.of(otherClassificationFile(), taxFileMissingField("year")))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @ParameterizedTest(name = "Inconclus si un champ requis manque ({0})")
    @ValueSource(strings = {"year", "nom", "rfr"})
    void inconclusive_missing_required_field(String field) {
        Document doc = Document.builder().files(List.of(taxFileMissingField(field))).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("Inconclus si parsedFile null")
    void inconclusive_null_parsed() {
        Document doc = Document.builder().files(List.of(taxFileNullParsed())).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Inconclus si premier fichier a analysis null (break empêche de voir ensuite un valide)")
    void inconclusive_break_on_null_analysis() {
        Document doc = Document.builder()
                .files(List.of(fileWithNullAnalysis(), taxFile(2023, "DUPONT", 12345)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Inconclus si premier fichier status FAILED (break)")
    void inconclusive_break_on_failed_analysis() {
        Document doc = Document.builder()
                .files(List.of(fileWithFailedAnalysis(), taxFile(2023, "DUPONT", 12345)))
                .build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}
