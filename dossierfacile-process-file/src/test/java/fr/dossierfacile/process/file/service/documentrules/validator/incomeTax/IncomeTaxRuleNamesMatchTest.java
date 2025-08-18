package fr.dossierfacile.process.file.service.documentrules.validator.incomeTax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import fr.dossierfacile.process.file.service.documentrules.validator.RuleValidatorOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class IncomeTaxRuleNamesMatchTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode baseQrNode(int year, String decl1Nom, String decl2Nom) {
        ObjectNode node = mapper.createObjectNode();
        node.put(TwoDDocDataType.ID_47.getLabel(), "NF1");
        node.put(TwoDDocDataType.ID_46.getLabel(), decl1Nom); // declarant1Nom
        node.put(TwoDDocDataType.ID_49.getLabel(), "NF2");
        if (decl2Nom != null) {
            node.put(TwoDDocDataType.ID_48.getLabel(), decl2Nom); // declarant2Nom
        } else {
            node.put(TwoDDocDataType.ID_48.getLabel(), "");
        }
        node.put(TwoDDocDataType.ID_45.getLabel(), String.valueOf(year));
        node.put(TwoDDocDataType.ID_43.getLabel(), "2");
        node.put(TwoDDocDataType.ID_4A.getLabel(), "20240101");
        node.put(TwoDDocDataType.ID_41.getLabel(), "12345");
        node.put(TwoDDocDataType.ID_44.getLabel(), "REF");
        return node;
    }

    private File taxAssessmentFile(int year, String decl1Nom, String decl2Nom, boolean twoDDoc) {
        BarCodeFileAnalysis.BarCodeFileAnalysisBuilder barBuilder = BarCodeFileAnalysis.builder()
                .documentType(BarCodeDocumentType.TAX_ASSESSMENT);
        if (twoDDoc) {
            barBuilder.barCodeType(BarCodeType.TWO_D_DOC).verifiedData(baseQrNode(year, decl1Nom, decl2Nom));
        } else {
            barBuilder.barCodeType(BarCodeType.QR_CODE); // pas 2D-Doc => fromQR vide
        }
        BarCodeFileAnalysis bar = barBuilder.build();

        // ParsedFileAnalysis non utilisé ici mais souvent présent (pas nécessaire pour cette règle)
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(null) // pas requis
                .build();

        File f = File.builder()
                .fileAnalysis(bar)
                .parsedFileAnalysis(pfa)
                .build();
        bar.setFile(f);
        pfa.setFile(f);
        return f;
    }

    private RuleValidatorOutput validate(Document d) {
        return new IncomeTaxRuleNamesMatch().validate(d);
    }

    @Test
    @DisplayName("Passe quand declarant1Nom correspond (LastName FirstName)")
    void pass_declarant1_match() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        File f = taxAssessmentFile(2023, "DUPONT JEAN", null, true);
        Document doc = Document.builder().tenant(tenant).files(List.of(f)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        Assertions.assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_TAX_NAMES);
    }

    @Test
    @DisplayName("Passe via preferredName utilisé comme nom de famille")
    void pass_preferred_name_match() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").preferredName("Martin").build();
        File f = taxAssessmentFile(2023, "MARTIN JEAN", null, true);
        Document doc = Document.builder().tenant(tenant).files(List.of(f)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Passe via declarant2Nom quand declarant1Nom ne matche pas")
    void pass_declarant2_match() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        // declarant1 mauvais, declarant2 bon
        File f = taxAssessmentFile(2023, "MARTIN PAUL", "DUPONT JEAN", true);
        Document doc = Document.builder().tenant(tenant).files(List.of(f)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isTrue();
    }

    @Test
    @DisplayName("Echec si aucun declarant ne correspond")
    void fail_no_match() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").preferredName("Martin").build();
        File f = taxAssessmentFile(2023, "DURAND PAUL", "MARTIN PAUL", true);
        Document doc = Document.builder().tenant(tenant).files(List.of(f)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
        Assertions.assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    @Test
    @DisplayName("Echec si TAX_ASSESSMENT mais pas 2D-Doc (fromQR vide)")
    void fail_tax_assessment_not_2d_doc() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        File f = taxAssessmentFile(2023, "DUPONT JEAN", null, false); // QR simple => fromQR empty => false
        Document doc = Document.builder().tenant(tenant).files(List.of(f)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }

    @Test
    @DisplayName("Plusieurs fichiers: un KO fait échouer la règle")
    void fail_when_one_file_ko() {
        Tenant tenant = Tenant.builder().firstName("Jean").lastName("Dupont").build();
        File ok = taxAssessmentFile(2023, "DUPONT JEAN", null, true);
        File ko = taxAssessmentFile(2023, "DURAND PAUL", null, true);
        Document doc = Document.builder().tenant(tenant).files(List.of(ok, ko)).build();
        RuleValidatorOutput out = validate(doc);
        Assertions.assertThat(out.isValid()).isFalse();
    }
}

