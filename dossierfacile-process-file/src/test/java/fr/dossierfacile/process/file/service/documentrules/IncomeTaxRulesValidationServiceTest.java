package fr.dossierfacile.process.file.service.documentrules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

class IncomeTaxRulesValidationServiceTest {


    private IncomeTaxRulesValidationService incomeTaxRulesValidationService = new IncomeTaxRulesValidationService();


    private File buildValidDfFileWithYear(int year) throws JsonProcessingException {
        BarCodeFileAnalysis barCodeFileAnalysis = BarCodeFileAnalysis.builder()
                .documentType(BarCodeDocumentType.TAX_ASSESSMENT)
                .barCodeType(BarCodeType.TWO_D_DOC)
                .verifiedData(
                        new ObjectMapper().readValue("""
                                       {
                                         "Déclarant 1": "DUPONT Jean",
                                         "Déclarant 2": "DUPONT Marie",
                                         "Nombre de parts": "2",
                                         "Année des revenus": "{2022}",
                                         "Date de mise en recouvrement": "30092023",
                                         "Revenu fiscal de référence": "42902",
                                         "Numéro fiscal du déclarant 1": "1234567890123",
                                         "Numéro fiscal du déclarant 2": "1234567890124",
                                         "Référence d’avis d’impôt": "2310A12345678"
                                       }                         
                                """.replace("{2022}", String.valueOf(year)), ObjectNode.class)
                )
                .build();
        return File.builder()
                .fileAnalysis(barCodeFileAnalysis)
                .build();
    }

    private Document buildValidTaxDocument() throws Exception {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("DUPONT")
                .build();
        List<File> files = new LinkedList<>();
        files.add(buildValidDfFileWithYear(LocalDate.now().minusMonths(21).getYear()));
        return Document.builder()
                .tenant(tenant)
                .documentCategory(DocumentCategory.TAX)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .files(files)
                .noDocument(true)
                .build();
    }

    @Test
    void document_full_test() throws Exception {
        Document document = buildValidTaxDocument();
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        incomeTaxRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_ok_with_two_file() throws Exception {
        Document document = buildValidTaxDocument();
        document.getFiles().add(buildValidDfFileWithYear(LocalDate.now().minusMonths(31).getYear()));
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        incomeTaxRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_full_test_with_preferredname() throws Exception {
        Document document = buildValidTaxDocument();
        document.getTenant().setPreferredName(document.getTenant().getLastName());
        document.getTenant().setLastName("AUTRE");

        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        incomeTaxRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_full_test_wrong_firstname() throws Exception {
        Document document = buildValidTaxDocument();
        document.getTenant().setFirstName("Joseph");
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        incomeTaxRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getBrokenRules()).hasSize(1);
        Assertions.assertThat(report.getBrokenRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_TAX_NAMES);
    }
}