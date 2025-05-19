package fr.dossierfacile.process.file.fileAnalysis.residencyTenant;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.fileAnalysis.*;
import fr.dossierfacile.process.file.fileAnalysis.DatasetLoader;
import fr.dossierfacile.process.file.fileAnalysis.config.OVHConfiguration;
import fr.dossierfacile.process.file.service.parsers.RentalReceipt3FParser;
import fr.dossierfacile.process.file.service.parsers.RentalReceiptParser;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.FFTBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.LaplacianBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.SobelBlurryAlgorithm;
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OVHConfiguration.class, DatasetLoader.class, LaplacianBlurryAlgorithm.class, SobelBlurryAlgorithm.class, FFTBlurryAlgorithm.class})
@TestPropertySource(locations = "/document_analysis.properties")
@EnabledIfEnvironmentVariable(named = "ENABLE_TESTS_FILE_ANALYSIS", matches = "true")
// See README.md for tutorial to start those test.
public class ResidencyTenantProcessorTest {

    @Autowired
    private TestOvhFileStorageServiceImpl ovhFileStorageService;

    private final RentalReceiptParser genericRentalParser = new RentalReceiptParser();
    private final RentalReceipt3FParser rental3fParser = new RentalReceipt3FParser();

    private static FileAnalysisTestData<Void, Void, Helper.FileDescription> testData = null;

    @BeforeAll
    static void initData(@Autowired DatasetLoader datasetLoader) throws IOException, ExecutionException, InterruptedException {
        testData = datasetLoader.loadDataset("tenant_proof/dataset.json", Helper.FileDescription.class, Void.class, Void.class);
    }

    @Nested
    class ValidResidencyTenantDocuments {

        static List<ValidDocumentData<Void, Helper.FileDescription>> validDocumentData() {
            return testData.getValidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("validDocumentData")
        void whenValidResidencyDocumentsShouldBeChecked(ValidDocumentData<Void, Helper.FileDescription> documentData) {
            var document = prepareDocument(documentData);
            var parsedFile = Helper.parseDocument(
                    documentData,
                    document.getFiles().getFirst(),
                    ovhFileStorageService,
                    genericRentalParser,
                    rental3fParser
            );
            assertThat(parsedFile).isNotNull();
            //noinspection DataFlowIssue
            assertThat(parsedFile.getTenantFullName()).isEqualTo(documentData.getFileDescription().tenantFullName());
            assertThat(parsedFile.getPeriod()).isEqualTo(documentData.getFileDescription().period());
            assertThat(parsedFile.getAmount()).isEqualTo(documentData.getFileDescription().amount());
        }
    }

    @Nested
    class InvalidResidencyTenantDocuments {

        static List<InvalidDocumentData<Void, Helper.FileDescription>> invalidDocumentData() {
            return testData.getInvalidDocuments();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidDocumentData")
        void whenNotParsableResidencyDocuments(InvalidDocumentData<Void, Helper.FileDescription> documentData) {
            var document = prepareDocument(documentData);
            var parsedFile = Helper.parseDocument(
                    documentData,
                    document.getFiles().getFirst(),
                    ovhFileStorageService,
                    genericRentalParser,
                    rental3fParser
            );
            assertThat(parsedFile).isNull();
        }
    }

    private Document prepareDocument(DocumentData<Helper.FileDescription> documentData) {

        var storageFileBuilder = StorageFile.builder()
                .id(1L);

        if (documentData.getBucketPath().contains(".pdf")) {
            storageFileBuilder.contentType("application/pdf");
        }

        var file = File.builder()
                .id(1L)
                .storageFile(storageFileBuilder.build())
                .build();

        var document = Document.builder()
                .id(1L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentStatus(DocumentStatus.TO_PROCESS)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .files(List.of(file))
                .build();
        file.setDocument(document);
        return document;
    }
}
