package fr.dossierfacile.process.file.service.documentrules.validator.payslip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.PayslipFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import fr.dossierfacile.common.entity.BarCodeType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class PayslipRuleCheckQRCodeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private File fileWithQrAndParsed(String fullname,
                                     YearMonth month,
                                     double parsedNet,
                                     double parsedCumulative,
                                     Double qrNetOverride,
                                     Double qrCumulativeOverride,
                                     String qrFullnameOverride,
                                     YearMonth qrMonthOverride) {
        // Build parsed payslip
        PayslipFile parsed = PayslipFile.builder()
                .classification(ParsedFileClassification.PUBLIC_PAYSLIP)
                .fullname(fullname)
                .month(month)
                .netTaxableIncome(parsedNet)
                .cumulativeNetTaxableIncome(parsedCumulative)
                .build();

        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.PUBLIC_PAYSLIP)
                .parsedFile(parsed)
                .build();

        // Prepare QR (2D-Doc) data
        String qrFullname = qrFullnameOverride != null ? qrFullnameOverride : fullname;
        YearMonth qrMonth = qrMonthOverride != null ? qrMonthOverride : month;
        double qrNet = qrNetOverride != null ? qrNetOverride : parsedNet;
        double qrCumulative = qrCumulativeOverride != null ? qrCumulativeOverride : parsedCumulative;

        LocalDate anyDayOfMonth = qrMonth.atDay(15); // milieu de mois
        String hexDate = fr.dossierfacile.process.file.util.TwoDDocUtil.get2DDocHexDateFromLocalDate(anyDayOfMonth);

        ObjectNode verifiedData = mapper.createObjectNode();
        verifiedData.put(TwoDDocDataType.ID_10.getLabel(), qrFullname); // fullname
        verifiedData.put(TwoDDocDataType.ID_54.getLabel(), hexDate); // end period date -> converted to YearMonth
        verifiedData.put(TwoDDocDataType.ID_58.getLabel(), String.valueOf(qrNet));
        verifiedData.put(TwoDDocDataType.ID_59.getLabel(), String.valueOf(qrCumulative));

        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .verifiedData(verifiedData)
                .barCodeType(BarCodeType.TWO_D_DOC)
                .build();

        File file = File.builder()
                .parsedFileAnalysis(pfa)
                .fileAnalysis(bar)
                .build();
        pfa.setFile(file);
        bar.setFile(file);
        return file;
    }

    private RuleValidatorOutput validate(boolean hasQr, Document d) {
        return new PayslipRuleCheckQRCode(hasQr).validate(d);
    }

    @Test
    @DisplayName("Passe quand le document n'est pas marqué comme contenant un QR (règle ignorée)")
    void pass_when_not_flagged_has_qr() {
        Document doc = Document.builder().files(List.of()).build();
        RuleValidatorOutput out = validate(false, doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_QRCHECK);
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("Passe quand données QR et parsed concordent")
    void pass_when_qr_and_parsed_match() {
        File f = fileWithQrAndParsed("JEAN DUPONT", YearMonth.now(), 2000.0, 6000.0, null, null, null, null);
        Document doc = Document.builder().files(List.of(f)).build();
        RuleValidatorOutput out = validate(true, doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec quand fullname différent")
    void fail_when_fullname_differs() {
        File f = fileWithQrAndParsed("JEAN DUPONT", YearMonth.now(), 2000.0, 6000.0, null, null, "PAUL DURAND", null);
        Document doc = Document.builder().files(List.of(f)).build();
        RuleValidatorOutput out = validate(true, doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec quand différence nette > 1")
    void fail_when_net_income_differs_over_tolerance() {
        File f = fileWithQrAndParsed("JEAN DUPONT", YearMonth.now(), 2000.0, 6000.0, 2002.2, null, null, null);
        Document doc = Document.builder().files(List.of(f)).build();
        RuleValidatorOutput out = validate(true, doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Passe quand différence cumul <= 1")
    void pass_when_cumulative_diff_within_1() {
        File f = fileWithQrAndParsed("JEAN DUPONT", YearMonth.now(), 2000.0, 6000.0, null, 6000.8, null, null);
        Document doc = Document.builder().files(List.of(f)).build();
        RuleValidatorOutput out = validate(true, doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec quand cumul QR = 0")
    void fail_when_qr_cumulative_is_zero() {
        File f = fileWithQrAndParsed("JEAN DUPONT", YearMonth.now(), 2000.0, 6000.0, null, 0.0, null, null);
        Document doc = Document.builder().files(List.of(f)).build();
        RuleValidatorOutput out = validate(true, doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Ignoré pour fichiers non public payslip -> passe")
    void pass_when_not_public_payslip() {
        // Construire un fichier parsed classification PAYSLIP (différent PUBLIC_PAYSLIP) -> la règle ignore
        PayslipFile parsed = PayslipFile.builder()
                .classification(ParsedFileClassification.PAYSLIP)
                .fullname("JEAN DUPONT")
                .month(YearMonth.now())
                .netTaxableIncome(2000.0)
                .cumulativeNetTaxableIncome(6000.0)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .classification(ParsedFileClassification.PAYSLIP)
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .parsedFile(parsed)
                .build();
        File f = File.builder().parsedFileAnalysis(pfa).build();
        pfa.setFile(f);
        Document doc = Document.builder().files(List.of(f)).build();
        RuleValidatorOutput out = validate(true, doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }
}

