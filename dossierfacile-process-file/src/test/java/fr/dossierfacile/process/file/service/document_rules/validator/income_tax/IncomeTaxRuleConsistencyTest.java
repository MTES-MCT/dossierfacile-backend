package fr.dossierfacile.process.file.service.document_rules.validator.income_tax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.service.document_rules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class IncomeTaxRuleConsistencyTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode qrNode(int year, String name, int rfr) {
        ObjectNode node = mapper.createObjectNode();
        node.put(TwoDDocDataType.ID_47.getLabel(), "NF1");
        node.put(TwoDDocDataType.ID_46.getLabel(), name); // declarant1Nom
        node.put(TwoDDocDataType.ID_49.getLabel(), "NF2");
        node.put(TwoDDocDataType.ID_48.getLabel(), "DECL2");
        node.put(TwoDDocDataType.ID_45.getLabel(), String.valueOf(year)); // annee revenus
        node.put(TwoDDocDataType.ID_43.getLabel(), "2");
        node.put(TwoDDocDataType.ID_4A.getLabel(), "20240101");
        node.put(TwoDDocDataType.ID_41.getLabel(), String.valueOf(rfr)); // revenu fiscal ref
        node.put(TwoDDocDataType.ID_44.getLabel(), "REF");
        return node;
    }

    private File taxFileWithQrAndParsed(int year, String name, int rfr) {
        // Barcode (TWO_D_DOC)
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .barCodeType(BarCodeType.TWO_D_DOC)
                .verifiedData(qrNode(year, name, rfr))
                .build();
        // Parsed analysis
        TaxIncomeMainFile parsed = TaxIncomeMainFile.builder()
                .anneeDesRevenus(year)
                .declarant1Nom(name)
                .revenuFiscalDeReference(rfr)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(parsed)
                .build();
        File f = File.builder()
                .fileAnalysis(bar)
                .parsedFileAnalysis(pfa)
                .build();
        bar.setFile(f);
        pfa.setFile(f);
        return f;
    }

    private File taxFileWithQrAndParsedMismatch(int year, String nameQr, int rfrQr, int yearParsed, String nameParsed, int rfrParsed) {
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .barCodeType(BarCodeType.TWO_D_DOC)
                .verifiedData(qrNode(year, nameQr, rfrQr))
                .build();
        TaxIncomeMainFile parsed = TaxIncomeMainFile.builder()
                .anneeDesRevenus(yearParsed)
                .declarant1Nom(nameParsed)
                .revenuFiscalDeReference(rfrParsed)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(parsed)
                .build();
        File f = File.builder().fileAnalysis(bar).parsedFileAnalysis(pfa).build();
        bar.setFile(f);
        pfa.setFile(f);
        return f;
    }

    private File taxFileMissingParsedField(int year, String name, int rfr, boolean nullYear, boolean nullName, boolean nullRfr) {
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .barCodeType(BarCodeType.TWO_D_DOC)
                .verifiedData(qrNode(year, name, rfr))
                .build();
        TaxIncomeMainFile.TaxIncomeMainFileBuilder builder = TaxIncomeMainFile.builder();
        builder.anneeDesRevenus(nullYear ? null : year)
                .declarant1Nom(nullName ? null : name)
                .revenuFiscalDeReference(nullRfr ? null : rfr);
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(builder.build())
                .build();
        File f = File.builder().fileAnalysis(bar).parsedFileAnalysis(pfa).build();
        bar.setFile(f);
        pfa.setFile(f);
        return f;
    }

    private File taxFileNoQrButParsed(int year, String name, int rfr) {
        // barCodeType différent => fromQR renvoie empty -> false
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .barCodeType(BarCodeType.QR_CODE)
                .build();
        TaxIncomeMainFile parsed = TaxIncomeMainFile.builder()
                .anneeDesRevenus(year)
                .declarant1Nom(name)
                .revenuFiscalDeReference(rfr)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(parsed)
                .build();
        File f = File.builder().fileAnalysis(bar).parsedFileAnalysis(pfa).build();
        bar.setFile(f);
        pfa.setFile(f);
        return f;
    }

    private File taxFileWithFailedAnalysis() {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.FAILED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(null)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        return f;
    }

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxRuleConsistency().validate(d);
    }

    @Test
    @DisplayName("Passe si QR et parsed cohérents (année, nom, RFR)")
    void pass_consistent() {
        File file = taxFileWithQrAndParsed(2023, "DUPONT", 12345);
        Document doc = Document.builder().files(List.of(file)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_FAKE);
    }

    @Test
    @DisplayName("Echec mismatch année")
    void fail_year_mismatch() {
        File file = taxFileWithQrAndParsedMismatch(2023, "DUPONT", 12345, 2022, "DUPONT", 12345);
        Document doc = Document.builder().files(List.of(file)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec mismatch nom (normalisation)")
    void fail_name_mismatch() {
        File file = taxFileWithQrAndParsedMismatch(2023, "DUPONT", 12345, 2023, "DURAND", 12345);
        Document doc = Document.builder().files(List.of(file)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Echec mismatch revenu fiscal")
    void fail_rfr_mismatch() {
        File file = taxFileWithQrAndParsedMismatch(2023, "DUPONT", 12345, 2023, "DUPONT", 99999);
        Document doc = Document.builder().files(List.of(file)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Passe si un champ requis manque côté parsed (condition de comparaison non déclenchée)")
    void pass_when_missing_parsed_field() {
        // annee null -> le bloc de comparaison n'est pas exécuté
        File file = taxFileMissingParsedField(2023, "DUPONT", 12345, true, false, false);
        Document doc = Document.builder().files(List.of(file)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec si pas de 2D-Doc (QR absent)")
    void fail_when_no_qr() {
        File file = taxFileNoQrButParsed(2023, "DUPONT", 12345);
        Document doc = Document.builder().files(List.of(file)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Passe car break sur analyse FAILED (aucune vérification)")
    void pass_break_on_failed_analysis() {
        File failed = taxFileWithFailedAnalysis();
        File consistent = taxFileWithQrAndParsed(2023, "DUPONT", 12345);
        // failed en premier -> break et la boucle ignore le second -> retourne true
        Document doc = Document.builder().files(List.of(failed, consistent)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }
}

