package fr.dossierfacile.process.file.service.documentrules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;

class GuaranteeProviderRulesValidationServiceTest {

    private GuaranteeProviderRulesValidationService guaranteeProviderRulesValidationService = new GuaranteeProviderRulesValidationService();

    Document buildGuaranteeProviderFile() throws JsonProcessingException {
        Tenant tenant = Tenant.builder()
                .firstName("Jean")
                .lastName("KALOUF")
                .build();
        GuaranteeProviderFile parsedFile = new ObjectMapper()
                .readValue("""
                        {
                            "names": [
                                {
                                    "lastName": "DUPONT",
                                    "firstName": "Mohamed"
                                },
                                {
                                    "lastName": "KALOUF",
                                     "firstName": "Jean"
                                }
                              ],
                            "signed": true,
                            "status": "COMPLETE",
                            "visaNumber": "V11000000000",
                            "deliveryDate": "01/12/2023",
                            "validityDate": "02/03/2024",
                            "classification": "GUARANTEE_PROVIDER"
                        }
                        """, GuaranteeProviderFile.class);
        ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder()
                .parsedFile(parsedFile)
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.GUARANTEE_PROVIDER)
                .build();
        File dfFile = File.builder()
                .parsedFileAnalysis(parsedFileAnalysis)
                .build();
        return Document.builder()
                .tenant(tenant)
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.CERTIFICATE_VISA)
                .files(Arrays.asList(dfFile))
                .build();
    }


    @Test
    public void document_full_test() throws Exception {
        Document document = buildGuaranteeProviderFile();
        ((GuaranteeProviderFile) document.getFiles().get(0).getParsedFileAnalysis().getParsedFile())
                .setValidityDate(LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        guaranteeProviderRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    public void document_expired() throws Exception {
        Document document = buildGuaranteeProviderFile();
        ((GuaranteeProviderFile) document.getFiles().get(0).getParsedFileAnalysis().getParsedFile())
                .setValidityDate(LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        guaranteeProviderRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getBrokenRules()).hasSize(1);
        Assertions.assertThat(report.getBrokenRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_GUARANTEE_EXIRED);
    }

    @Test
    public void document_wrong_firstname() throws Exception {
        Document document = buildGuaranteeProviderFile();
        document.getTenant().setFirstName("Michel");
        ((GuaranteeProviderFile) document.getFiles().get(0).getParsedFileAnalysis().getParsedFile())
                .setValidityDate(LocalDate.now().plusDays(15).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .brokenRules(new LinkedList<>())
                .build();
        guaranteeProviderRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getBrokenRules()).hasSize(1);
        Assertions.assertThat(report.getBrokenRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_GUARANTEE_NAMES);
    }
}