package fr.dossierfacile.document.analysis.rule.validator.professional;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.rule.NamesRuleData;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalNamesRuleTest {

    private final ProfessionalNamesRule rule = new ProfessionalNamesRule();

    @Test
    void should_pass_when_tenant_name_matches_2ddoc() {
        // Given
        Tenant tenant = Tenant.builder()
                .firstName("DIALLA BAH")
                .lastName("KONATE")
                .build();

        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        Document document = buildDocumentWithTenant(tenant, List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(output.isBlocking()).isFalse();
    }

    @Test
    void should_pass_when_guarantor_name_matches_2ddoc() {
        // Given
        Guarantor guarantor = Guarantor.builder()
                .firstName("DIALLA BAH")
                .lastName("KONATE")
                .build();

        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        Document document = buildDocumentWithGuarantor(guarantor, List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(output.isBlocking()).isFalse();
    }

    @Test
    void should_pass_when_preferred_name_matches_2ddoc() {
        // Given - Tenant has preferredName "KONATE" while birth lastName is "DIALLO"
        Tenant tenant = Tenant.builder()
                .firstName("DIALLA BAH")
                .lastName("DIALLO")
                .preferredName("KONATE")
                .build();

        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        Document document = buildDocumentWithTenant(tenant, List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(output.isBlocking()).isFalse();
    }

    @Test
    void should_deduplicate_identical_extracted_names_from_multiple_2ddocs() {
        // Given - 2 barcodes with identical extracted name
        Tenant tenant = Tenant.builder()
                .firstName("DIALLA BAH")
                .lastName("KONATE")
                .build();

        BarcodeModel barcode1 = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        BarcodeModel barcode2 = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        Document document = buildDocumentWithTenant(tenant, List.of(barcode1, barcode2));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        NamesRuleData ruleData = (NamesRuleData) output.rule().getRuleData();
        assertThat(ruleData.extractedNames()).hasSize(1);
        assertThat(ruleData.extractedNames().get(0).firstNames()).isEqualTo("DIALLA BAH");
        assertThat(ruleData.extractedNames().get(0).lastName()).isEqualTo("KONATE");
    }

    @Test
    void should_keep_distinct_extracted_names_from_multiple_2ddocs() {
        // Given - 2 barcodes with 2 different extracted names
        Tenant tenant = Tenant.builder()
                .firstName("DIALLA BAH")
                .lastName("KONATE")
                .build();

        BarcodeModel barcode1 = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        BarcodeModel barcode2 = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("JEAN").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("DUPONT").build()
                ))
                .build();

        Document document = buildDocumentWithTenant(tenant, List.of(barcode1, barcode2));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        NamesRuleData ruleData = (NamesRuleData) output.rule().getRuleData();
        assertThat(ruleData.extractedNames()).hasSize(2);
    }

    @Test
    void should_fail_when_firstname_matches_but_lastname_does_not() {
        // Given
        Tenant tenant = Tenant.builder()
                .firstName("DIALLA BAH")
                .lastName("SMITH")
                .build();

        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        Document document = buildDocumentWithTenant(tenant, List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.isBlocking()).isFalse();
    }

    @Test
    void should_fail_when_lastname_matches_but_firstname_does_not() {
        // Given
        Tenant tenant = Tenant.builder()
                .firstName("JEAN")
                .lastName("KONATE")
                .build();

        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        Document document = buildDocumentWithTenant(tenant, List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.isBlocking()).isFalse();
    }

    @Test
    void should_be_inconclusive_when_neither_tenant_nor_guarantor_attached() {
        // Given - Document with no Tenant and no Guarantor
        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of(
                        GenericProperty.builder().name("liste_prenoms").type(GenericProperty.TYPE_STRING).value("DIALLA BAH").build(),
                        GenericProperty.builder().name("nom_patronymique").type(GenericProperty.TYPE_STRING).value("KONATE").build()
                ))
                .build();

        Document document = buildDocumentWithBarcodesOnly(List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        assertThat(output.isBlocking()).isFalse();
    }

    @Test
    void should_be_inconclusive_when_no_barcode_identities() {
        // Given
        Tenant tenant = Tenant.builder()
                .firstName("DIALLA BAH")
                .lastName("KONATE")
                .build();

        BarcodeModel barcode = BarcodeModel.builder()
                .docType("29")
                .typedData(List.of())
                .build();

        Document document = buildDocumentWithTenant(tenant, List.of(barcode));

        // When
        RuleValidatorOutput output = rule.validate(document);

        // Then
        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        assertThat(output.isBlocking()).isFalse();
    }

    private Document buildDocumentWithTenant(Tenant tenant, List<BarcodeModel> barcodes) {
        ResultModel resultModel = ResultModel.builder()
                .barcodes(barcodes)
                .build();

        DocumentIAFileAnalysis analysis = DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(resultModel)
                .build();

        File file = File.builder()
                .documentIAFileAnalysis(analysis)
                .build();

        return Document.builder()
                .tenant(tenant)
                .files(List.of(file))
                .build();
    }

    private Document buildDocumentWithGuarantor(Guarantor guarantor, List<BarcodeModel> barcodes) {
        ResultModel resultModel = ResultModel.builder()
                .barcodes(barcodes)
                .build();

        DocumentIAFileAnalysis analysis = DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(resultModel)
                .build();

        File file = File.builder()
                .documentIAFileAnalysis(analysis)
                .build();

        return Document.builder()
                .guarantor(guarantor)
                .files(List.of(file))
                .build();
    }

    private Document buildDocumentWithBarcodesOnly(List<BarcodeModel> barcodes) {
        ResultModel resultModel = ResultModel.builder()
                .barcodes(barcodes)
                .build();

        DocumentIAFileAnalysis analysis = DocumentIAFileAnalysis.builder()
                .analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS)
                .result(resultModel)
                .build();

        File file = File.builder()
                .documentIAFileAnalysis(analysis)
                .build();

        return Document.builder()
                .files(List.of(file))
                .build();
    }
}
