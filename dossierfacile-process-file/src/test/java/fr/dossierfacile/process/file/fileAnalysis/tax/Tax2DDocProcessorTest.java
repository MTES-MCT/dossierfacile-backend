package fr.dossierfacile.process.file.fileAnalysis.tax;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import fr.dossierfacile.fileAnalysis.*;
import fr.dossierfacile.process.file.barcode.InMemoryFile;
import fr.dossierfacile.process.file.barcode.InMemoryImageFile;
import fr.dossierfacile.process.file.barcode.InMemoryPdfFile;
import fr.dossierfacile.process.file.barcode.twoddoc.validation.AntsTrustServiceList;
import fr.dossierfacile.process.file.barcode.twoddoc.validation.TwoDDocCertificationAuthorities;
import fr.dossierfacile.process.file.fileAnalysis.DatasetLoader;
import fr.dossierfacile.process.file.fileAnalysis.config.OVHConfiguration;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.FFTBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.LaplacianBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.SobelBlurryAlgorithm;
import fr.dossierfacile.process.file.service.qrcodeanalysis.TwoDDocFileAuthenticator;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        OVHConfiguration.class,
        DatasetLoader.class,
        LaplacianBlurryAlgorithm.class,
        SobelBlurryAlgorithm.class,
        FFTBlurryAlgorithm.class,
        TwoDDocFileAuthenticator.class,
        ObjectMapper.class,
        TwoDDocCertificationAuthorities.class,
        AntsTrustServiceList.class,
        RestTemplate.class
})
@TestPropertySource(locations = "/document_analysis.properties")
@EnabledIfEnvironmentVariable(named = "ENABLE_TESTS_FILE_ANALYSIS", matches = "true")
// See README.md for tutorial to start those test.
public class Tax2DDocProcessorTest {

    @Autowired
    private TestOvhFileStorageServiceImpl ovhFileStorageService;

    @Autowired
    private TwoDDocFileAuthenticator twoDDocFileAuthenticator;

    private static FileAnalysisTestData<Void, Void, Void> testData = null;

    @BeforeAll
    static void initData(@Autowired DatasetLoader datasetLoader) throws IOException, ExecutionException, InterruptedException {
        testData = datasetLoader.loadDataset("my_name_tax/dataset.json", Void.class, Void.class, Void.class);
    }

    @Nested
    class ValidTaxDocuments {

        static List<ValidDocumentData<Void, Void>> validDocumentData() {
            return testData.getValidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("validDocumentData")
        void whenValidTaxDocument(ValidDocumentData<Void, Void> documentData) throws IOException {
            var inMemoryFile = getInMemoryFile(documentData);

            assertThat(inMemoryFile.hasQrCode()).isFalse();
            assertThat(inMemoryFile.has2DDoc()).isTrue();

            var twodDocAnalysis = twoDDocFileAuthenticator.analyze(inMemoryFile.get2DDoc());
            assertThat(twodDocAnalysis).isNotNull();
            assertThat(twodDocAnalysis.getDocumentType()).isEqualTo(BarCodeDocumentType.TAX_ASSESSMENT);
            assertThat(twodDocAnalysis.getAuthenticationStatus()).isEqualTo(FileAuthenticationStatus.VALID);
            assertThat(twodDocAnalysis.getVerifiedData()).isNotNull();
        }
    }

    @Nested
    class InvalidTaxDocuments {

        static List<InvalidDocumentData<Void, Void>> invalidDocumentData() {
            return testData.getInvalidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidDocumentData")
        void whenValidTaxDocument(InvalidDocumentData<Void, Void> documentData) throws IOException {

            var inMemoryFile = getInMemoryFile(documentData);

            assertThat(inMemoryFile.hasQrCode()).isFalse();
            assertThat(inMemoryFile.has2DDoc()).isFalse();
        }
    }

    private InMemoryFile getInMemoryFile(DocumentData<Void> documentData) throws IOException {
        java.io.File tmpFile;
        try {
            tmpFile = ovhFileStorageService.downloadAsync(documentData.getBucketPath()).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        InMemoryFile inMemoryFile = null;
        try (InputStream inputStream = new FileInputStream(tmpFile)){
            if (documentData.getBucketPath().endsWith(".pdf")) {
                inMemoryFile = new InMemoryPdfFile(Loader.loadPDF(inputStream.readAllBytes()));
            } else {
                inMemoryFile = new InMemoryImageFile(ImageIO.read(inputStream));
            }
        }
        return inMemoryFile;
    }
}
