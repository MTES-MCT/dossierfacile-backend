package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootTest
class PdfGeneratorServiceImplTest {

    @Autowired
    PdfGeneratorService pdfGeneratorService;

    @MockBean
    ApartmentSharingCommonService apartmentSharingCommonService;

    @MockBean
    TenantCommonRepository tenantRepository;
    @MockBean
    StorageFileRepository storageFileRepository;


    @Value("${mock.storage.path}")
    private String filePath;

    private File file;

    @BeforeEach
    void init() {
        Mockito.when(storageFileRepository.save(Mockito.any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void generateFullDossierPdf() throws IOException {
        ApartmentSharing apartmentSharing = buildApartmentSharing();
        Mockito.when(apartmentSharingCommonService.findById(1L)).thenReturn(Optional.of(apartmentSharing));
        Mockito.when(apartmentSharingCommonService.save(ArgumentMatchers.any())).thenAnswer(i -> i.getArguments()[0]);

        Mockito.when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(1L)).thenReturn(0);

        pdfGeneratorService.generateFullDossierPdf(1L);


        Assertions.assertEquals(FileStatus.COMPLETED, apartmentSharing.getDossierPdfDocumentStatus());
        Assertions.assertNotNull( apartmentSharing.getPdfDossierFile());

        file = new File( filePath +  apartmentSharing.getPdfDossierFile().getPath());
        Assertions.assertTrue (file.exists());
    }

    @AfterEach
    void tearDown() {
        file.delete();
    }

    private ApartmentSharing buildApartmentSharing() {
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(1L);
        apartmentSharing.setToken("7fdad8b1-6a9e-40dc-acab-d698701f76db");
        apartmentSharing.setTokenPublic("da566c0f-1ef7-4906-8a9f-cab667c9434d");
        apartmentSharing.setApplicationType(ApplicationType.ALONE);
        Tenant tenant = buildTenant();
        apartmentSharing.setTenants(List.of(tenant));

        return apartmentSharing;
    }

    private Tenant buildTenant() {
        Tenant tenant = Tenant.builder().build();
        tenant.setId(1L);
        tenant.setFirstName("Dr");
        tenant.setLastName("Who");
        tenant.setGuarantors(new ArrayList<>());
        tenant.setStatus(TenantFileStatus.VALIDATED);
        tenant.setHonorDeclaration(true);
        tenant.setTenantType(TenantType.CREATE);
        tenant.setEmail("dr@tardis.fr");
        tenant.setClarification("""
                Bonjour test truc anrs anruiste anruise tanruiset arnusiet arnusi etaurnsiet arnuiset
                anruise tanruiset anruise tanrusiet anruiset anrusiet anruset nrauist rste aurnsiet anrusiet anrsa ute narusix rauia anuxrisa aurnanruise tanruiset anruise tanrusiet anruiset anrusiet anruset nrauist rste aurnsiet anrusiet anrsa ute narusix rauia anuxrisa aurn

                test smiley \uD83D\uDE08 auie
                test smiley \uD83D\uDE08 xi
                """);

        List<Document> documents = buildDocuments(tenant);

        tenant.setDocuments(documents);

        return tenant;
    }

    private List<Document> buildDocuments(Tenant tenant) {
        Document professional = new Document();
        professional.setDocumentCategory(DocumentCategory.PROFESSIONAL);
        professional.setDocumentSubCategory(DocumentSubCategory.CDI);
        professional.setDocumentStatus(DocumentStatus.VALIDATED);
        professional.setTenant(tenant);
        professional.setWatermarkFile(StorageFile.builder().path("CNI.pdf").build());

        Document financial = new Document();
        financial.setDocumentCategory(DocumentCategory.FINANCIAL);
        financial.setDocumentSubCategory(DocumentSubCategory.SALARY);
        financial.setDocumentStatus(DocumentStatus.VALIDATED);
        financial.setTenant(tenant);
        financial.setMonthlySum(3000);
        financial.setWatermarkFile(StorageFile.builder().path("CNI.pdf").build());

        Document tax = new Document();
        tax.setDocumentCategory(DocumentCategory.TAX);
        tax.setDocumentSubCategory(DocumentSubCategory.LESS_THAN_YEAR);
        tax.setDocumentStatus(DocumentStatus.VALIDATED);
        tax.setTenant(tenant);
        tax.setWatermarkFile(StorageFile.builder().path("CNI.pdf").build());

        Document identification = new Document();
        identification.setDocumentCategory(DocumentCategory.IDENTIFICATION);
        identification.setDocumentSubCategory(DocumentSubCategory.FRENCH_PASSPORT);
        identification.setDocumentStatus(DocumentStatus.VALIDATED);
        identification.setTenant(tenant);
        identification.setWatermarkFile(StorageFile.builder().path("CNI.pdf").build());

        Document residency = new Document();
        residency.setDocumentCategory(DocumentCategory.RESIDENCY);
        residency.setDocumentSubCategory(DocumentSubCategory.TENANT);
        residency.setDocumentStatus(DocumentStatus.VALIDATED);
        residency.setTenant(tenant);
        residency.setWatermarkFile(StorageFile.builder().path("CNI.pdf").build());

        return List.of(professional, financial, tax, identification, residency);
    }
}
