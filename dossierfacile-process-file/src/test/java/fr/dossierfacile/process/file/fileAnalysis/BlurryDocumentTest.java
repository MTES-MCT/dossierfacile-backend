package fr.dossierfacile.process.file.fileAnalysis;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.ocr.BlurryAlgorithmType;
import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import fr.dossierfacile.common.repository.BlurryFileAnalysisRepository;
import fr.dossierfacile.fileAnalysis.*;
import fr.dossierfacile.process.file.fileAnalysis.config.OVHConfiguration;
import fr.dossierfacile.process.file.fileAnalysis.config.OpenCVConfiguration;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.StorageFileLoaderService;
import fr.dossierfacile.process.file.service.documentrules.BlurryRulesValidationService;
import fr.dossierfacile.process.file.service.processors.blurry.BlurryProcessor;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.DifferenceOfGaussiansBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.FFTBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.LaplacianBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.SobelBlurryAlgorithm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        OVHConfiguration.class,
        OpenCVConfiguration.class,
        BlurryProcessor.class,
        FFTBlurryAlgorithm.class,
        LaplacianBlurryAlgorithm.class,
        SobelBlurryAlgorithm.class,
        StorageFileLoaderService.class,
        BlurryFileAnalysisRepository.class,
        BlurryRulesValidationService.class,
        DifferenceOfGaussiansBlurryAlgorithm.class,
        FileRepository.class,
        DatasetLoader.class
})
@TestPropertySource(locations = {"/document_analysis.properties", "classpath:application-dev.properties"})
@EnabledIfEnvironmentVariable(named = "ENABLE_TESTS_FILE_ANALYSIS", matches = "true")
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

    private static final Logger logger = (Logger) LoggerFactory.getLogger(BlurryDocumentTest.class);

    @BeforeAll
    static void init() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%msg%n");
        ple.setContext(lc);
        ple.start();

        var fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setFile("testResult/blurryTest.log");
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();
        logger.addAppender(fileAppender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false); /* set to true if root should log too */
    }

    private static FileAnalysisTestData<Void, Void, Void> testData = null;

    @BeforeAll
    static void initData(@Autowired DatasetLoader datasetLoader) throws IOException, ExecutionException, InterruptedException {
        testData = datasetLoader.loadDataset("blurryFiles/dataset-documents.json", Void.class, Void.class, Void.class);
    }

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
        assert result.getBlurryFileAnalysis() != null;
        var sobelScore = result.getBlurryFileAnalysis().getBlurryResults().stream().filter(blurryResult -> blurryResult.algorithm() == BlurryAlgorithmType.SOBEL).findFirst().orElseThrow().score();
        var laplacian = result.getBlurryFileAnalysis().getBlurryResults().stream().filter(blurryResult -> blurryResult.algorithm() == BlurryAlgorithmType.LAPLACIEN).findFirst().orElseThrow().score();
        var fftScore = result.getBlurryFileAnalysis().getBlurryResults().stream().filter(blurryResult -> blurryResult.algorithm() == BlurryAlgorithmType.FFT).findFirst().orElseThrow().score();
        var dogScore = result.getBlurryFileAnalysis().getBlurryResults().stream().filter(blurryResult -> blurryResult.algorithm() == BlurryAlgorithmType.DOG).findFirst().orElseThrow().score();
        logger.info("DOC: {} | Sobel: {} | Laplacian: {} | FFT: {} | DOG: {}", documentData.getBucketPath(), sobelScore, laplacian, fftScore, dogScore);
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

