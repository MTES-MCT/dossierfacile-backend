package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.api.pdfgenerator.util.parameterresolvers.ApartmentSharingResolver;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
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
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@SpringBootTest
@ExtendWith(ApartmentSharingResolver.class)
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
    void generateFullDossierPdf(ApartmentSharing apartmentSharing) throws IOException {
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

}
