package fr.dossierfacile.process.file.fileAnalysis;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.GuaranteeProviderFile;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.fileAnalysis.*;
import fr.dossierfacile.process.file.service.documentrules.GuaranteeProviderRulesValidationService;
import fr.dossierfacile.process.file.service.parsers.GuaranteeVisaleParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OvhConfiguration.class})
@TestPropertySource(locations = "/document_analysis.properties")
@EnabledIfEnvironmentVariable(named = "ENABLE_TESTS_FILE_ANALYSIS", matches = "true")
// See README.md for tutorial to start those test.
public class GuaranteeProviderDocumentTest {

    @Autowired
    private TestOvhFileStorageServiceImpl ovhFileStorageService;

    private final GuaranteeProviderRulesValidationService guaranteeProviderRulesValidationService = new GuaranteeProviderRulesValidationService();
    private final GuaranteeVisaleParser parser = new GuaranteeVisaleParser();

    private static final FileAnalysisTestData<Void, ExpectedError, FileDescription> testData = new FileAnalysisTestData<>(
            "guarantee_provider_file_analysis",
            DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE,
            List.of(DocumentSubCategory.OTHER_GUARANTEE, DocumentSubCategory.VISALE),
            List.of(
                    new ValidDocumentData<>(
                            "guarantee_provider_certificate/visale/valide/001.pdf",
                            new FileDescription(List.of(new TenantName("Killian", "HERZER-RENAMBOT"), new TenantName("VIOLAINE", "SIMOND"))),
                            null
                    ),
                    new ValidDocumentData<>(
                            "guarantee_provider_certificate/visale/valide/002.pdf",
                            new FileDescription(List.of(new TenantName("Jihane", "YUKSEK"), new TenantName("Arthur", "ROBIN"))),
                            null
                    ),
                    new ValidDocumentData<>(
                            "guarantee_provider_certificate/visale/valide/003.pdf",
                            new FileDescription(List.of(new TenantName("Othmane", "AOUASSAR"))),
                            null
                    ),
                    new ValidDocumentData<>(
                            "guarantee_provider_certificate/visale/valide/004.pdf",
                            new FileDescription(List.of(new TenantName("Anaël", "CLOAREC FAVEREAU"))),
                            null
                    ),
                    new ValidDocumentData<>(
                            "guarantee_provider_certificate/visale/valide/005.pdf",
                            new FileDescription(List.of(new TenantName("Gladys", "PLAGNE"), new TenantName("Elouane", "PLAGNE"))),
                            null
                    ),
                    new ValidDocumentData<>(
                            "guarantee_provider_certificate/visale/valide/006.pdf",
                            new FileDescription(List.of(new TenantName("Jihane", "YUKSEK"), new TenantName("Arthur", "ROBIN"))),
                            null
                    ),
                    new ValidDocumentData<>(
                            "guarantee_provider_certificate/visale/valide/009.pdf",
                            new FileDescription(List.of(new TenantName("Nadine", "CHERON"))),
                            null
                    ),
                    new ValidDocumentData<>(
                            "guarantee_provider_certificate/visale/valide/010.pdf",
                            new FileDescription(List.of(new TenantName("Alisha Rachel", "KURIAN"), new TenantName("Nikhil", "Johnson Vallavanatt"))),
                            null
                    )
            ),
            List.of(
                    new InvalidDocumentData<>(
                            "guarantee_provider_certificate/visale/invalide/002.pdf",
                            new FileDescription(null),
                            new NotParsableDocument()
                    ),
                    new InvalidDocumentData<>(
                            "guarantee_provider_certificate/visale/invalide/008.pdf",
                            new FileDescription(null),
                            new NotParsableDocument()
                    ),
                    new InvalidDocumentData<>(
                            "guarantee_provider_certificate/visale/invalide/011.pdf",
                            new FileDescription(List.of(new TenantName("Anafël", "CLOARfEC"))),
                            new WrongNamesDocument()
                    ),
                    new InvalidDocumentData<>(
                            "guarantee_provider_certificate/visale/invalide/012.pdf",
                            new FileDescription(List.of(new TenantName("Joelle", "FERZLY"))),
                            new ExpiredDocument()
                    )
            )
    );

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
                            .brokenRules(new ArrayList<>())
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

        static List<InvalidDocumentData<NotParsableDocument, FileDescription>> notParsableDocuments() {
            return testData.getInvalidDocuments()
                    .stream()
                    .filter(item -> item.getExpectedError() instanceof NotParsableDocument)
                    .map(InvalidVisaleDocuments::<NotParsableDocument>cast)
                    .collect(Collectors.toList());
        }

        static List<InvalidDocumentData<WrongNamesDocument, FileDescription>> wrongNameDocuments() {
            return testData.getInvalidDocuments()
                    .stream()
                    .filter(item -> item.getExpectedError() instanceof WrongNamesDocument)
                    .map(InvalidVisaleDocuments::<WrongNamesDocument>cast)
                    .collect(Collectors.toList());
        }

        static List<InvalidDocumentData<ExpiredDocument, FileDescription>> expiredDocuments() {
            return testData.getInvalidDocuments()
                    .stream()
                    .filter(item -> item.getExpectedError() instanceof ExpiredDocument)
                    .map(InvalidVisaleDocuments::<ExpiredDocument>cast)
                    .collect(Collectors.toList());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notParsableDocuments")
        void whenNotParsableDocumentsParsedFileShouldBeNull(InvalidDocumentData<NotParsableDocument, FileDescription> documentData) {
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
        void whenWrongNamesDocumentsExpectDeniedStatus(InvalidDocumentData<WrongNamesDocument, FileDescription> documentData) {
            try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
                LocalDate currentDate = LocalDate.of(2025, 4, 15);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                try {
                    var parsedFile = getParsedDocument(documentData);
                    var document = prepareDocument(parsedFile, documentData);

                    var documentAnalysisReport = DocumentAnalysisReport.builder()
                            .document(document)
                            .brokenRules(new ArrayList<>())
                            .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                            .id(1L)
                            .build();

                    guaranteeProviderRulesValidationService.process(document, documentAnalysisReport);
                    assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
                    assertThat(documentAnalysisReport.getBrokenRules().size()).isEqualTo(1);
                    assertThat(documentAnalysisReport.getBrokenRules().getFirst().getRule()).isEqualTo(DocumentRule.R_GUARANTEE_NAMES);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("expiredDocuments")
        void whenExpiredDocumentsExpectDeniedStatus(InvalidDocumentData<WrongNamesDocument, FileDescription> documentData) {
            try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
                LocalDate currentDate = LocalDate.of(2025, 4, 15);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                try {
                    var parsedFile = getParsedDocument(documentData);
                    var document = prepareDocument(parsedFile, documentData);

                    var documentAnalysisReport = DocumentAnalysisReport.builder()
                            .document(document)
                            .brokenRules(new ArrayList<>())
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

    sealed static class ExpectedError {
    }

    static final class NotParsableDocument extends ExpectedError {
    }

    static final class WrongNamesDocument extends  ExpectedError {
    }

    static final class ExpiredDocument extends  ExpectedError {
    }

    record TenantName(String firstName, String lastName) {
    }

    record FileDescription(List<TenantName> tenantNames) {
    }
}
