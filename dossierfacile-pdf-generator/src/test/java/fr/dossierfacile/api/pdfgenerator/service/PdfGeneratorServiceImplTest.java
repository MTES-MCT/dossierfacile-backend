package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.repository.ApartmentSharingRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.repository.TenantCommonRepository;
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

@SpringBootTest
class PdfGeneratorServiceImplTest {

    @Autowired
    PdfGeneratorService pdfGeneratorService;

    @MockBean
    ApartmentSharingRepository repository;

    @MockBean
    TenantCommonRepository tenantRepository;

    @Value("${mock.storage.path}")
    private String filePath;

    @Test
    void generateFullDossierPdf() throws IOException {
        File file = new File(filePath + "/7fdad8b1-6a9e-40dc-acab-d698701f76db.pdf");
        try {
            file.delete();
        } catch (Exception e) {
            // Normal case
        }

        ApartmentSharing apartmentSharing = buildApartmentSharing();
        Mockito.when(repository.getById(1L)).thenReturn(apartmentSharing);
        Mockito.when(repository.save(ArgumentMatchers.any())).thenAnswer(i -> i.getArguments()[0]);

        Mockito.when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(1L)).thenReturn(0);

        pdfGeneratorService.generateFullDossierPdf(1L);

        file = new File(filePath + "/7fdad8b1-6a9e-40dc-acab-d698701f76db.pdf");
        assert (file.exists());
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
        tenant.setAllowCheckTax(true);
        tenant.setHonorDeclaration(true);
        tenant.setTenantType(TenantType.CREATE);
        tenant.setEmail("dr@tardis.fr");

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
        professional.setName("CNI.pdf");

        Document financial = new Document();
        financial.setDocumentCategory(DocumentCategory.FINANCIAL);
        financial.setDocumentSubCategory(DocumentSubCategory.SALARY);
        financial.setDocumentStatus(DocumentStatus.VALIDATED);
        financial.setTenant(tenant);
        financial.setMonthlySum(3000);
        financial.setName("CNI.pdf");

        Document tax = new Document();
        tax.setDocumentCategory(DocumentCategory.TAX);
        tax.setDocumentSubCategory(DocumentSubCategory.LESS_THAN_YEAR);
        tax.setDocumentStatus(DocumentStatus.VALIDATED);
        tax.setTenant(tenant);
        tax.setName("CNI.pdf");

        Document identification = new Document();
        identification.setDocumentCategory(DocumentCategory.IDENTIFICATION);
        identification.setDocumentSubCategory(DocumentSubCategory.FRENCH_PASSPORT);
        identification.setDocumentStatus(DocumentStatus.VALIDATED);
        identification.setTenant(tenant);
        identification.setName("CNI.pdf");

        Document residency = new Document();
        residency.setDocumentCategory(DocumentCategory.RESIDENCY);
        residency.setDocumentSubCategory(DocumentSubCategory.TENANT);
        residency.setDocumentStatus(DocumentStatus.VALIDATED);
        residency.setTenant(tenant);
        residency.setName("CNI.pdf");

        return List.of(professional, financial, tax, identification, residency);
    }
}