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
import java.util.Collections;
import java.util.LinkedList;

class GuaranteeProviderRulesValidationServiceTest {

    private final GuaranteeProviderRulesValidationService guaranteeProviderRulesValidationService = new GuaranteeProviderRulesValidationService();

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
                            "signed": null,
                            "status": null,
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
                .tenant(null)
                .guarantor(Guarantor.builder().tenant(tenant).build())
                .documentCategory(DocumentCategory.IDENTIFICATION)
                .documentSubCategory(DocumentSubCategory.OTHER_GUARANTEE)
                .files(Collections.singletonList(dfFile))
                .build();
    }


    @Test
    void document_full_test() throws Exception {
        Document document = buildGuaranteeProviderFile();
        ((GuaranteeProviderFile) document.getFiles().get(0).getParsedFileAnalysis().getParsedFile())
                .setValidityDate(LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .failedRules(new LinkedList<>())
                .build();
        guaranteeProviderRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_expired() throws Exception {
        Document document = buildGuaranteeProviderFile();
        ((GuaranteeProviderFile) document.getFiles().get(0).getParsedFileAnalysis().getParsedFile())
                .setValidityDate(LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .failedRules(new LinkedList<>())
                .build();
        guaranteeProviderRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getFailedRules()).hasSize(1);
        Assertions.assertThat(report.getFailedRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_GUARANTEE_EXPIRED);
    }

    @Test
    void document_valid_preferredname() throws Exception {
        Document document = buildGuaranteeProviderFile();
        document.getGuarantor().getTenant().setLastName("NAKAMURA");
        document.getGuarantor().getTenant().setPreferredName("KALOUF");
        ((GuaranteeProviderFile) document.getFiles().get(0).getParsedFileAnalysis().getParsedFile())
                .setValidityDate(LocalDate.now().plusDays(15).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .failedRules(new LinkedList<>())
                .build();
        guaranteeProviderRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void document_wrong_firstname() throws Exception {
        Document document = buildGuaranteeProviderFile();
        document.getGuarantor().getTenant().setFirstName("Michel");
        ((GuaranteeProviderFile) document.getFiles().get(0).getParsedFileAnalysis().getParsedFile())
                .setValidityDate(LocalDate.now().plusDays(15).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .document(document)
                .failedRules(new LinkedList<>())
                .build();
        guaranteeProviderRulesValidationService.process(document, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
        Assertions.assertThat(report.getFailedRules()).hasSize(1);
        Assertions.assertThat(report.getFailedRules().get(0)).matches(docRule -> docRule.getRule() == DocumentRule.R_GUARANTEE_NAMES);
    }
}