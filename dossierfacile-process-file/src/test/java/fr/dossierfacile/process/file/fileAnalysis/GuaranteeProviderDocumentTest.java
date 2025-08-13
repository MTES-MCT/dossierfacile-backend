package fr.dossierfacile.process.file.fileAnalysis;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.fileAnalysis.*;
import fr.dossierfacile.process.file.service.documentrules.GuaranteeProviderRulesValidationService;
import fr.dossierfacile.process.file.service.parsers.GuaranteeVisaleParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OvhConfiguration.class, DatasetLoader.class})
@TestPropertySource(locations = "/document_analysis.properties")
@EnabledIfEnvironmentVariable(named = "ENABLE_TESTS_FILE_ANALYSIS", matches = "true")
// See README.md for tutorial to start those test.
public class GuaranteeProviderDocumentTest {

    @Autowired
    private TestOvhFileStorageServiceImpl ovhFileStorageService;

    private final GuaranteeProviderRulesValidationService guaranteeProviderRulesValidationService = new GuaranteeProviderRulesValidationService();
    private final GuaranteeVisaleParser parser = new GuaranteeVisaleParser();

    private static FileAnalysisTestData<Void, ExpectedError, FileDescription> testData = null;

    @BeforeAll
    static void initData(@Autowired DatasetLoader datasetLoader) throws IOException, ExecutionException, InterruptedException {
        testData = datasetLoader.loadDataset("guarantee_provider_certificate/visale/dataset.json", FileDescription.class, Void.class, ExpectedError.class);
    }

    private GuaranteeProviderFile getParsedDocument(DocumentData documentData) throws IOException {
        File tmpFile;
        try {
            tmpFile = ovhFileStorageService.downloadAsync(documentData.getBucketPath()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        var data = parser.parse(tmpFile);
        tmpFile.deleteOnExit();
        return data;
    }

    @Nested
    class ValidVisaleDocuments {

        static List<ValidDocumentData<Void, FileDescription>> validVisaleDocuments() {
            return testData.getValidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("validVisaleDocuments")
        void whenValidVisaleDocumentsShouldBeChecked(ValidDocumentData<Void, FileDescription> documentData) {

            try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
                LocalDate currentDate = LocalDate.of(2025, 4, 15);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                try {
                    var parsedFile = getParsedDocument(documentData);
                    var document = prepareDocument(parsedFile, documentData);

                    var documentAnalysisReport = DocumentAnalysisReport.builder()
                            .document(document)
                            .failedRules(new ArrayList<>())
                            .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                            .id(1L)
                            .build();

                    guaranteeProviderRulesValidationService.process(document, documentAnalysisReport);
                    assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Nested
    class InvalidVisaleDocuments {

        @SuppressWarnings("unchecked")
        private static <E extends ExpectedError> InvalidDocumentData<E, FileDescription> cast(InvalidDocumentData<?, FileDescription> data) {
            return (InvalidDocumentData<E, FileDescription>) data;
        }

        static List<InvalidDocumentData<ExpectedError, FileDescription>> notParsableDocuments() {
            return testData.getInvalidDocuments()
                    .stream()
                    .filter(item -> item.getExpectedError().errorType.equals("NotParsableDocument"))
                    .collect(Collectors.toList());
        }

        static List<InvalidDocumentData<ExpectedError, FileDescription>> wrongNameDocuments() {
            return testData.getInvalidDocuments()
                    .stream()
                    .filter(item -> item.getExpectedError().errorType.equals("WrongNamesDocument"))
                    .collect(Collectors.toList());
        }

        static List<InvalidDocumentData<ExpectedError, FileDescription>> expiredDocuments() {
            return testData.getInvalidDocuments()
                    .stream()
                    .filter(item -> item.getExpectedError().errorType.equals("ExpiredDocument"))
                    .collect(Collectors.toList());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notParsableDocuments")
        void whenNotParsableDocumentsParsedFileShouldBeNull(InvalidDocumentData<ExpectedError, FileDescription> documentData) {
            try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
                LocalDate currentDate = LocalDate.of(2025, 4, 15);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                try {
                    var parsedFile = getParsedDocument(documentData);
                    assertThat(parsedFile).isNull();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("wrongNameDocuments")
        void whenWrongNamesDocumentsExpectDeniedStatus(InvalidDocumentData<ExpectedError, FileDescription> documentData) {
            try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
                LocalDate currentDate = LocalDate.of(2025, 4, 15);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                try {
                    var parsedFile = getParsedDocument(documentData);
                    var document = prepareDocument(parsedFile, documentData);

                    var documentAnalysisReport = DocumentAnalysisReport.builder()
                            .document(document)
                            .failedRules(new ArrayList<>())
                            .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                            .id(1L)
                            .build();

                    guaranteeProviderRulesValidationService.process(document, documentAnalysisReport);
                    assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
                    assertThat(documentAnalysisReport.getFailedRules().size()).isEqualTo(1);
                    assertThat(documentAnalysisReport.getFailedRules().getFirst().getRule()).isEqualTo(DocumentRule.R_GUARANTEE_NAMES);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("expiredDocuments")
        void whenExpiredDocumentsExpectDeniedStatus(InvalidDocumentData<ExpectedError, FileDescription> documentData) {
            try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
                LocalDate currentDate = LocalDate.of(2025, 4, 15);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                try {
                    var parsedFile = getParsedDocument(documentData);
                    var document = prepareDocument(parsedFile, documentData);

                    var documentAnalysisReport = DocumentAnalysisReport.builder()
                            .document(document)
                            .failedRules(new ArrayList<>())
                            .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                            .id(1L)
                            .build();

                    guaranteeProviderRulesValidationService.process(document, documentAnalysisReport);
                    assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Document prepareDocument(GuaranteeProviderFile parsedFile, DocumentData<FileDescription> documentData) {
        var parsedFileAnalysis = ParsedFileAnalysis.builder()
                .id(1L)
                .parsedFile(parsedFile)
                .classification(parsedFile == null ? ParsedFileClassification.GUARANTEE_PROVIDER : parsedFile.getClassification())
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .build();

        var file = fr.dossierfacile.common.entity.File.builder()
                .id(1L)
                .parsedFileAnalysis(parsedFileAnalysis)
                .build();

        var tenant = Tenant.builder()
                .id(1L)
                .firstName(documentData.getFileDescription().tenantNames.getFirst().firstName)
                .lastName(documentData.getFileDescription().tenantNames.getFirst().lastName)
                .build();

        var guarantor = Guarantor.builder()
                .typeGuarantor(TypeGuarantor.ORGANISM)
                .id(1L)
                .tenant(tenant)
                .build();

        var document = Document.builder()
                .id(1L)
                .documentCategory(DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE)
                .documentStatus(DocumentStatus.TO_PROCESS)
                .documentSubCategory(DocumentSubCategory.VISALE)
                .files(List.of(file))
                .guarantor(guarantor)
                .build();

        parsedFileAnalysis.setFile(file);
        file.setDocument(document);

        return document;
    }

    record TenantName(String firstName, String lastName) {
    }

    record FileDescription(List<TenantName> tenantNames) {
    }

    record ExpectedError(String errorType) {
    }
}
