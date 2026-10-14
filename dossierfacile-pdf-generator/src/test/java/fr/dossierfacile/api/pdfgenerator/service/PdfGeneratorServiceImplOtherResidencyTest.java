package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.model.FileInputStream;
import fr.dossierfacile.api.pdfgenerator.repository.DocumentRepository;
import fr.dossierfacile.api.pdfgenerator.repository.FileRepository;
import fr.dossierfacile.api.pdfgenerator.service.templates.ApartmentSharingPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.BOIdentificationPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.BOPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.EmptyBOPdfDocumentTemplate;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.repository.WatermarkDocumentRepository;
import fr.dossierfacile.common.service.interfaces.EncryptionKeyService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfGeneratorServiceImplOtherResidencyTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private WatermarkDocumentRepository watermarkDocumentRepository;
    @Mock
    private StorageFileRepository storageFileRepository;
    @Mock
    private EmptyBOPdfDocumentTemplate emptyBOPdfDocumentTemplate;
    @Mock
    private BOPdfDocumentTemplate boPdfDocumentTemplate;
    @Mock
    private BOIdentificationPdfDocumentTemplate identificationPdfDocumentTemplate;
    @Mock
    private ApartmentSharingPdfDocumentTemplate apartmentSharingPdfDocumentTemplate;
    @Mock
    private ApartmentSharingPdfDossierFileGenerationServiceImpl pdfFileGenerationService;
    @Mock
    private EncryptionKeyService encryptionKeyService;
    @Mock
    private DocumentRepository documentRepository;

    private PdfGeneratorServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new PdfGeneratorServiceImpl(fileRepository, fileStorageService, watermarkDocumentRepository, storageFileRepository,
                emptyBOPdfDocumentTemplate, boPdfDocumentTemplate, identificationPdfDocumentTemplate, apartmentSharingPdfDocumentTemplate,
                pdfFileGenerationService, encryptionKeyService, documentRepository);
        when(fileStorageService.upload(any(), any())).thenAnswer(i -> i.getArguments()[1]);
    }

    @Test
    void explanationOnlyRendersTheEmptyTemplate() throws Exception {
        Document document = otherResidencyDocument(true);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(fileRepository.findAllByDocumentId(1L)).thenReturn(List.of());
        when(emptyBOPdfDocumentTemplate.render(document)).thenReturn(new ByteArrayInputStream(new byte[0]));

        service.generateBOPdfDocument(1L);

        verify(emptyBOPdfDocumentTemplate).render(document);
        verify(boPdfDocumentTemplate, never()).render(anyList());
    }

    @Test
    void explanationWithFilesRendersTheExplanationPageThenTheFiles() throws Exception {
        Document document = otherResidencyDocument(false);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(fileRepository.findAllByDocumentId(1L)).thenReturn(List.of(pdfFile()));
        when(fileStorageService.download(any())).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(emptyBOPdfDocumentTemplate.render(document)).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(boPdfDocumentTemplate.render(anyList())).thenReturn(new ByteArrayInputStream(new byte[0]));

        service.generateBOPdfDocument(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileInputStream>> captor = ArgumentCaptor.forClass(List.class);
        verify(boPdfDocumentTemplate).render(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getMediaType()).isEqualTo(MediaType.APPLICATION_PDF);
    }

    private static Document otherResidencyDocument(boolean noDocument) {
        Tenant tenant = Tenant.builder().id(10L).build();
        Guarantor guarantor = Guarantor.builder().id(20L).tenant(tenant).build();
        return Document.builder()
                .id(1L)
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.OTHER_RESIDENCY)
                .noDocument(noDocument)
                .customText("Mon garant vit chez son employeur")
                .guarantor(guarantor)
                .build();
    }

    private static File pdfFile() {
        StorageFile storageFile = StorageFile.builder().id(5L).contentType(MediaType.APPLICATION_PDF_VALUE).build();
        return File.builder().id(3L).storageFile(storageFile).build();
    }
}
