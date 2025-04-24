package fr.dossierfacile.process.file.fileAnalysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.repository.BlurryFileAnalysisRepository;
import fr.dossierfacile.fileAnalysis.*;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.StorageFileLoaderService;
import fr.dossierfacile.process.file.service.documentrules.BlurryRulesValidationService;
import fr.dossierfacile.process.file.service.processors.blurry.BlurryProcessor;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.FFTBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.LaplacianBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.SobelBlurryAlgorithm;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TestProperties.class,
        BlurryProcessor.class,
        FFTBlurryAlgorithm.class,
        LaplacianBlurryAlgorithm.class,
        SobelBlurryAlgorithm.class,
        StorageFileLoaderService.class,
        BlurryFileAnalysisRepository.class,
        BlurryRulesValidationService.class,
        FileRepository.class
})
@TestPropertySource(locations = {"/document_analysis.properties", "classpath:application-dev.properties"})
//@EnabledIfEnvironmentVariable(named = "ENABLE_TESTS_FILE_ANALYSIS", matches = "true")
public class BlurryDocumentTest {

    @Autowired
    private TestOvhFileStorageServiceImpl ovhFileStorageService;

    @Autowired
    private BlurryProcessor blurryProcessor;

    @Autowired
    private BlurryRulesValidationService blurryRulesValidationService;

    @MockBean
    private BlurryFileAnalysisRepository blurryFileAnalysisRepository;

    @MockBean
    private StorageFileLoaderService storageFileLoaderService;

    @MockBean
    private FileRepository fileRepository;

    private static final FileAnalysisTestData<Void, Void, Void> testData = new FileAnalysisTestData<>(
            "blurryFiles",
            DocumentCategory.NULL,
            List.of(),
            List.of(
                    new ValidDocumentData<>(
                            "blurryFiles/valide/001.pdf"
                    )
            ),
            List.of(
                    new InvalidDocumentData<>(
                            "blurryFiles/invalide/001.png"
                    ),
                    new InvalidDocumentData<>(
                            "blurryFiles/invalide/002.pdf"
                    ),
                    new InvalidDocumentData<>(
                            "blurryFiles/invalide/003.jpeg"
                    )
            )
    );

    private File getFile(String bucketPath) throws ExecutionException, InterruptedException {
        return ovhFileStorageService.downloadAsync(bucketPath).get();
    }

    private Document getDocumentWithBlurryAnalysis(DocumentData<Void> documentData) throws ExecutionException, InterruptedException {
        var document = Document.builder()
                .id(1L)
                .build();

        var dtFile = fr.dossierfacile.common.entity.File.builder()
                .id(1L)
                .document(document)
                .build();

        document.setFiles(List.of(dtFile));

        when(storageFileLoaderService.getTemporaryFilePath(any())).thenReturn(getFile(documentData.getBucketPath()));
        var result = blurryProcessor.process(dtFile);
        assertThat(result).isNotNull();
        assertThat(result.getBlurryFileAnalysis()).isNotNull();
        assertThat(result.getBlurryFileAnalysis().getAnalysisStatus()).isEqualTo(BlurryFileAnalysisStatus.COMPLETED);
        return document;
    }

    @Nested
    class BlurryDocumentsTest {

        static List<InvalidDocumentData<Void, Void>> blurryFiles() {
            return testData.getInvalidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("blurryFiles")
        void whenBlurryFiles(InvalidDocumentData<Void, Void> documentData) throws ExecutionException, InterruptedException {
            var document = getDocumentWithBlurryAnalysis(documentData);
            assertThat(document.getFiles().getFirst().getBlurryFileAnalysis()).isNotNull();

            var documentAnalysisReport = DocumentAnalysisReport.builder()
                    .id(1L)
                    .document(document)
                    .brokenRules(new ArrayList<>())
                    .build();

            blurryRulesValidationService.process(document, documentAnalysisReport);
            assertThat(documentAnalysisReport.getBrokenRules()).hasSize(1);
        }
    }

    @Nested
    class ValidDocumentTest {
        static List<ValidDocumentData<Void, Void>> notBlurryFiles() {
            return testData.getValidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notBlurryFiles")
        void whenNotBlurryFiles(ValidDocumentData<Void, Void> documentData) throws ExecutionException, InterruptedException {
            var document = getDocumentWithBlurryAnalysis(documentData);
            assertThat(document.getFiles().getFirst().getBlurryFileAnalysis()).isNotNull();

            var documentAnalysisReport = DocumentAnalysisReport.builder()
                    .id(1L)
                    .document(document)
                    .brokenRules(new ArrayList<>())
                    .build();

            blurryRulesValidationService.process(document, documentAnalysisReport);
            assertThat(documentAnalysisReport.getBrokenRules()).isEmpty();
        }
    }
}

