package fr.dossierfacile.process.file.fileAnalysis;

import fr.dossierfacile.common.enums.BlurryFileAnalysisStatus;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.repository.BlurryFileAnalysisRepository;
import fr.dossierfacile.fileAnalysis.FileAnalysisTestData;
import fr.dossierfacile.fileAnalysis.InvalidDocumentData;
import fr.dossierfacile.fileAnalysis.TestOvhFileStorageServiceImpl;
import fr.dossierfacile.fileAnalysis.ValidDocumentData;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.StorageFileLoaderService;
import fr.dossierfacile.process.file.service.processors.blurry.BlurryProcessor;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.FFTBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.LaplacianBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.SobelBlurryAlgorithm;
import fr.dossierfacile.process.file.util.ImageUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
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
        FileRepository.class,
        DatasetLoader.class
})
@TestPropertySource(locations = {"/document_analysis.properties", "classpath:application-dev.properties"})
@EnabledIfEnvironmentVariable(named = "ENABLE_TESTS_FILE_ANALYSIS", matches = "true")
public class BlurryProcessorTests {

    @Autowired
    private TestOvhFileStorageServiceImpl ovhFileStorageService;

    @Autowired
    private BlurryProcessor blurryProcessor;

    @MockBean
    private BlurryFileAnalysisRepository blurryFileAnalysisRepository;

    @MockBean
    private StorageFileLoaderService storageFileLoaderService;

    @MockBean
    private FileRepository fileRepository;

    private static FileAnalysisTestData<Void, Void, Void> testData = null;

    @BeforeAll
    static void initData(@Autowired DatasetLoader datasetLoader) throws IOException, ExecutionException, InterruptedException {
        testData = datasetLoader.loadDataset("blurryFiles/dataset-processor.json", Void.class, Void.class, Void.class);
    }

    private File getFile(String bucketPath) throws ExecutionException, InterruptedException {
        return ovhFileStorageService.downloadAsync(bucketPath).get();
    }

    @Nested
    class BlurryFiles {
        static List<InvalidDocumentData<Void, Void>> blurryFiles() {
            return testData.getInvalidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("blurryFiles")
        void testBlurryFiles(InvalidDocumentData<Void, Void> blurryFile) {
            var dtFile = fr.dossierfacile.common.entity.File.builder()
                    .id(1L)
                    .build();

            try {
                when(storageFileLoaderService.getTemporaryFilePath(any())).thenReturn(getFile(blurryFile.getBucketPath()));
                var result = blurryProcessor.process(dtFile);
                assertThat(result).isNotNull();
                assertThat(result.getBlurryFileAnalysis()).isNotNull();
                assertThat(result.getBlurryFileAnalysis().getAnalysisStatus()).isEqualTo(BlurryFileAnalysisStatus.COMPLETED);
                System.out.println(result.getBlurryFileAnalysis().getBlurryResults());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    class NotBlurryFiles {
        static List<ValidDocumentData<Void, Void>> notBlurryFiles() {
            return testData.getValidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("notBlurryFiles")
        void testNotBlurryFiles(ValidDocumentData<Void, Void> blurryFile) {
            var dtFile = fr.dossierfacile.common.entity.File.builder()
                    .id(1L)
                    .build();

            try {
                when(storageFileLoaderService.getTemporaryFilePath(any())).thenReturn(getFile(blurryFile.getBucketPath()));
                var result = blurryProcessor.process(dtFile);
                assertThat(result).isNotNull();
                assertThat(result.getBlurryFileAnalysis()).isNotNull();
                assertThat(result.getBlurryFileAnalysis().getAnalysisStatus()).isEqualTo(BlurryFileAnalysisStatus.COMPLETED);
                System.out.println(result.getBlurryFileAnalysis().getBlurryResults());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    class exceptionTest {

        @Test
        void testExceptionWhileGettingImages() {
            var dtFile = fr.dossierfacile.common.entity.File.builder()
                    .id(1L)
                    .build();
            var testFile = testData.getValidDocuments().getFirst();
            try (MockedStatic<ImageUtils> imageUtilsMockedStatic = mockStatic(ImageUtils.class)) {
                imageUtilsMockedStatic.when(() -> ImageUtils.getImagesFromFile(any())).thenThrow(new IOException());
                when(storageFileLoaderService.getTemporaryFilePath(any())).thenReturn(getFile(testFile.getBucketPath()));
                var result = blurryProcessor.process(dtFile);
                assertThat(result).isNotNull();
                assertThat(result.getBlurryFileAnalysis()).isNotNull();
                assertThat(result.getBlurryFileAnalysis().getAnalysisStatus()).isEqualTo(BlurryFileAnalysisStatus.FAILED);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
