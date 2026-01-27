package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.api.pdfgenerator.util.parameterresolvers.ApartmentSharingResolver;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.repository.ApplicationLogRepository;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.MailCommonService;
import fr.dossierfacile.logging.job.LogAggregator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@SpringBootTest(properties = "brevo.enabled=false")
@ExtendWith(ApartmentSharingResolver.class)
class PdfGeneratorServiceImplTest {

    @Autowired
    PdfGeneratorService pdfGeneratorService;

    @MockitoBean
    ApartmentSharingCommonService apartmentSharingCommonService;

    @MockitoBean
    LogAggregator logAggregator;

    @MockitoBean
    TenantCommonRepository tenantRepository;

    @MockitoBean
    StorageFileRepository storageFileRepository;

    @MockitoBean
    ApplicationLogRepository applicationLogRepository;

    @MockitoBean
    MailCommonService mailCommonService;

    @Value("${mock.storage.path}")
    private String filePath;

    private File file;

    @BeforeEach
    void init() {
        Mockito.when(storageFileRepository.save(Mockito.any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void generateFullDossierPdf(ApartmentSharing apartmentSharing) throws IOException {
        Mockito.when(apartmentSharingCommonService.findById(1L)).thenReturn(Optional.of(apartmentSharing));
        Mockito.when(apartmentSharingCommonService.save(ArgumentMatchers.any())).thenAnswer(i -> i.getArguments()[0]);

        Mockito.when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(1L)).thenReturn(0);

        pdfGeneratorService.generateFullDossierPdf(1L);

        Assertions.assertEquals(FileStatus.COMPLETED, apartmentSharing.getDossierPdfDocumentStatus());
        Assertions.assertNotNull(apartmentSharing.getPdfDossierFile());

        file = new File(filePath + apartmentSharing.getPdfDossierFile().getPath());
        Assertions.assertTrue(file.exists());
    }

    @AfterEach
    void tearDown() {
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
