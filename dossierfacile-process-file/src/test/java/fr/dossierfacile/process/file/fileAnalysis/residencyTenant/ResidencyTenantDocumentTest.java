package fr.dossierfacile.process.file.fileAnalysis.residencyTenant;

import com.google.common.collect.Streams;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.*;
import fr.dossierfacile.fileAnalysis.FileAnalysisTestData;
import fr.dossierfacile.fileAnalysis.TestOvhFileStorageServiceImpl;
import fr.dossierfacile.fileAnalysis.ValidDocumentData;
import fr.dossierfacile.process.file.fileAnalysis.DatasetLoader;
import fr.dossierfacile.process.file.fileAnalysis.config.OVHConfiguration;
import fr.dossierfacile.process.file.service.documentrules.RentalReceiptRulesValidationService;
import fr.dossierfacile.process.file.service.parsers.RentalReceipt3FParser;
import fr.dossierfacile.process.file.service.parsers.RentalReceiptParser;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.FFTBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.LaplacianBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.SobelBlurryAlgorithm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OVHConfiguration.class, DatasetLoader.class, LaplacianBlurryAlgorithm.class, SobelBlurryAlgorithm.class, FFTBlurryAlgorithm.class})
@TestPropertySource(locations = "/document_analysis.properties")
@EnabledIfEnvironmentVariable(named = "ENABLE_TESTS_FILE_ANALYSIS", matches = "true")
class ResidencyTenantDocumentTest {

    @Autowired
    private TestOvhFileStorageServiceImpl ovhFileStorageService;

    private final RentalReceiptParser genericRentalParser = new RentalReceiptParser();
    private final RentalReceipt3FParser rental3fParser = new RentalReceipt3FParser();

    private static FileAnalysisTestData<Helper.TenantInformation, Void, Helper.FileDescription> testData = null;

    private final RentalReceiptRulesValidationService rentalReceiptRulesValidationService = new RentalReceiptRulesValidationService();

    @BeforeAll
    static void initData(@Autowired DatasetLoader datasetLoader) throws IOException, ExecutionException, InterruptedException {
        testData = datasetLoader.loadDataset("tenant_proof/dataset.json", Helper.FileDescription.class, Helper.TenantInformation.class, Void.class);
    }

    @Test
    void whenValidResidencyDocumentNoAnalysError() {
        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            try (MockedStatic<YearMonth> mockedStatic1 = Mockito.mockStatic(YearMonth.class, Mockito.CALLS_REAL_METHODS)) {
                YearMonth yearMonth = YearMonth.of(2025, 5);
                LocalDate currentDate = LocalDate.of(2025, 5, 1);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                mockedStatic1.when(YearMonth::now).thenReturn(yearMonth);

                var listOfValidDocuments = testData.getValidDocuments().stream().filter(item ->
                        item.getBucketPath().equals("tenant_proof/valide/002.pdf") || item.getBucketPath().equals("tenant_proof/valide/003.pdf") || item.getBucketPath().equals("tenant_proof/valide/004.pdf")
                ).toList();
                var document = prepareDocument(listOfValidDocuments);
                document.getFiles().forEach(file -> {
                    assertThat(file.getParsedFileAnalysis()).isNotNull();
                });

                var documentAnalysisReport = DocumentAnalysisReport.builder()
                        .document(document)
                        .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                        .brokenRules(new ArrayList<>())
                        .id(1L)
                        .build();

                rentalReceiptRulesValidationService.process(document, documentAnalysisReport);
                assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
                assertThat(documentAnalysisReport.getBrokenRules()).isEmpty();

            }
        }
    }

    @Test
    void whenValidResidencyDocumentButWrongPageNumber() {
        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            try (MockedStatic<YearMonth> mockedStatic1 = Mockito.mockStatic(YearMonth.class, Mockito.CALLS_REAL_METHODS)) {
                YearMonth yearMonth = YearMonth.of(2025, 5);
                LocalDate currentDate = LocalDate.of(2025, 5, 1);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                mockedStatic1.when(YearMonth::now).thenReturn(yearMonth);

                var listOfValidDocuments = testData.getValidDocuments().stream().filter(item ->
                        item.getBucketPath().equals("tenant_proof/valide/001.pdf")
                ).toList();
                var document = prepareDocument(listOfValidDocuments);
                document.getFiles().forEach(file -> {
                    assertThat(file.getParsedFileAnalysis()).isNotNull();
                });

                var documentAnalysisReport = DocumentAnalysisReport.builder()
                        .document(document)
                        .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                        .brokenRules(new ArrayList<>())
                        .id(1L)
                        .build();

                rentalReceiptRulesValidationService.process(document, documentAnalysisReport);
                assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
                assertThat(documentAnalysisReport.getBrokenRules().size()).isEqualTo(2);
                assertThat(documentAnalysisReport.getBrokenRules().getFirst().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_NB_DOCUMENTS);
            }
        }
    }

    @Test
    void whenValidResidencyDocumentButMissMatchNames() {
        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            try (MockedStatic<YearMonth> mockedStatic1 = Mockito.mockStatic(YearMonth.class, Mockito.CALLS_REAL_METHODS)) {
                YearMonth yearMonth = YearMonth.of(2025, 5);
                LocalDate currentDate = LocalDate.of(2025, 5, 1);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                mockedStatic1.when(YearMonth::now).thenReturn(yearMonth);

                var listOfValidDocuments = testData.getValidDocuments().stream().filter(item ->
                        item.getBucketPath().equals("tenant_proof/valide/002.pdf") || item.getBucketPath().equals("tenant_proof/valide/003.pdf") || item.getBucketPath().equals("tenant_proof/valide/004.pdf")
                ).toList();
                var customTenant = Tenant.builder()
                        .id(1L)
                        .firstName("John")
                        .lastName("Doe")
                        .build();
                var document = prepareDocument(listOfValidDocuments, customTenant, false);
                document.getFiles().forEach(file -> assertThat(file.getParsedFileAnalysis()).isNotNull());

                var documentAnalysisReport = DocumentAnalysisReport.builder()
                        .document(document)
                        .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                        .brokenRules(new ArrayList<>())
                        .id(1L)
                        .build();

                rentalReceiptRulesValidationService.process(document, documentAnalysisReport);
                assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
                assertThat(documentAnalysisReport.getBrokenRules().size()).isEqualTo(1);
                assertThat(documentAnalysisReport.getBrokenRules().getFirst().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_NAME);
            }
        }
    }

    @Test
    void whenValidResidencyDocumentButTooOld() {
        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            try (MockedStatic<YearMonth> mockedStatic1 = Mockito.mockStatic(YearMonth.class, Mockito.CALLS_REAL_METHODS)) {
                YearMonth yearMonth = YearMonth.of(2025, 12);
                LocalDate currentDate = LocalDate.of(2025, 12, 1);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                mockedStatic1.when(YearMonth::now).thenReturn(yearMonth);

                var listOfValidDocuments = testData.getValidDocuments().stream().filter(item ->
                        item.getBucketPath().equals("tenant_proof/valide/002.pdf") || item.getBucketPath().equals("tenant_proof/valide/003.pdf") || item.getBucketPath().equals("tenant_proof/valide/004.pdf")
                ).toList();
                var document = prepareDocument(listOfValidDocuments);
                document.getFiles().forEach(file -> assertThat(file.getParsedFileAnalysis()).isNotNull());

                var documentAnalysisReport = DocumentAnalysisReport.builder()
                        .document(document)
                        .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                        .brokenRules(new ArrayList<>())
                        .id(1L)
                        .build();

                rentalReceiptRulesValidationService.process(document, documentAnalysisReport);
                assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
                assertThat(documentAnalysisReport.getBrokenRules().size()).isEqualTo(1);
                assertThat(documentAnalysisReport.getBrokenRules().getFirst().getRule()).isEqualTo(DocumentRule.R_RENT_RECEIPT_MONTHS);
            }
        }
    }

    @Test
    void whenValidResidencyDocumentForGuarantorNoError() {
        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            try (MockedStatic<YearMonth> mockedStatic1 = Mockito.mockStatic(YearMonth.class, Mockito.CALLS_REAL_METHODS)) {
                YearMonth yearMonth = YearMonth.of(2025, 4);
                LocalDate currentDate = LocalDate.of(2025, 4, 27);
                mockedStatic.when(LocalDate::now).thenReturn(currentDate);
                mockedStatic1.when(YearMonth::now).thenReturn(yearMonth);

                var listOfValidDocuments = testData.getValidDocuments().stream().filter(item ->
                        item.getBucketPath().equals("tenant_proof/valide/002.pdf")
                ).toList();
                var document = prepareDocument(listOfValidDocuments, true);
                document.getFiles().forEach(file -> assertThat(file.getParsedFileAnalysis()).isNotNull());

                var documentAnalysisReport = DocumentAnalysisReport.builder()
                        .document(document)
                        .analysisStatus(DocumentAnalysisStatus.UNDEFINED)
                        .brokenRules(new ArrayList<>())
                        .id(1L)
                        .build();

                rentalReceiptRulesValidationService.process(document, documentAnalysisReport);
                assertThat(documentAnalysisReport.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
                assertThat(documentAnalysisReport.getBrokenRules().size()).isZero();
            }
        }
    }

    private Document prepareDocument(List<ValidDocumentData<Helper.TenantInformation, Helper.FileDescription>> documentData) {
        return prepareDocument(documentData, null, false);
    }

    private Document prepareDocument(List<ValidDocumentData<Helper.TenantInformation, Helper.FileDescription>> documentData, boolean isForGuarantor) {
        return prepareDocument(documentData, null, isForGuarantor);
    }

    @SuppressWarnings("UnstableApiUsage")
    private Document prepareDocument(List<ValidDocumentData<Helper.TenantInformation, Helper.FileDescription>> documentData, Tenant customTenant, Boolean isForGuarantor) {

        Tenant tenant = null;

        var apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.ALONE)
                .build();

        var guarantor = Guarantor.builder()
                .id(1L)
                .firstName(documentData.getFirst().getExpectedResult().tenantFirstName())
                .lastName(documentData.getFirst().getExpectedResult().tenantLastName())
                .build();

        if (customTenant == null) {
            tenant = Tenant.builder()
                    .id(1L)
                    .firstName(documentData.getFirst().getExpectedResult().tenantFirstName())
                    .lastName(documentData.getFirst().getExpectedResult().tenantLastName())
                    .apartmentSharing(apartmentSharing)
                    .build();
        } else {
            tenant = customTenant;
            tenant.setApartmentSharing(apartmentSharing);
        }

        apartmentSharing.setTenants(List.of(tenant));

        var document = Document.builder()
                .id(1L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentStatus(DocumentStatus.TO_PROCESS)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .documentCategoryStep(DocumentCategoryStep.TENANT_RECEIPT)
                .build();

        if (isForGuarantor) {
            document.setGuarantor(guarantor);
        } else {
            document.setTenant(tenant);
        }

        var fileList = Streams.mapWithIndex(documentData.stream(), (item, index) -> {
            var storageFile = StorageFile.builder()
                    .id(index);

            if (item.getBucketPath().endsWith(".pdf")) {
                storageFile.contentType("application/pdf");
            } else {
                storageFile.contentType("image/jpeg");
            }

            var fileDto = File.builder()
                    .id(index)
                    .storageFile(storageFile.build())
                    .numberOfPages(item.getFileDescription().numberOfPage())
                    .document(document)
                    .build();

            var parseFileAnalysis = ParsedFileAnalysis.builder()
                    .id(index)
                    .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                    .classification(ParsedFileClassification.RENTAL_RECEIPT)
                    .parsedFile(Helper.parseDocument(
                            item,
                            fileDto,
                            ovhFileStorageService,
                            genericRentalParser,
                            rental3fParser
                    ))
                    .build();

            parseFileAnalysis.setFile(fileDto);
            fileDto.setParsedFileAnalysis(parseFileAnalysis);

            return fileDto;

        }).toList();

        document.setFiles(fileList);

        return document;
    }

}