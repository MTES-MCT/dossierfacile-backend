package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.model.FileInputStream;
import fr.dossierfacile.api.pdfgenerator.repository.FileRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.api.pdfgenerator.service.templates.ApartmentSharingPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.BOIdentificationPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.BOPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.EmptyBOPdfDocumentTemplate;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.WatermarkDocument;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.repository.WatermarkDocumentRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.dossierfacile.common.enums.DocumentCategory.IDENTIFICATION;
import static fr.dossierfacile.common.enums.DocumentCategory.IDENTIFICATION_LEGAL_PERSON;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorServiceImpl implements PdfGeneratorService {
    private static final String EXCEPTION = "Sentry ID Exception: ";

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final WatermarkDocumentRepository watermarkDocumentRepository;
    private final StorageFileRepository storageFileRepository;
    private final EmptyBOPdfDocumentTemplate emptyBOPdfDocumentTemplate;
    private final BOPdfDocumentTemplate boPdfDocumentTemplate;
    @Qualifier("boIdentificationPdfDocumentTemplate")
    private final BOIdentificationPdfDocumentTemplate identificationPdfDocumentTemplate;
    private final ApartmentSharingPdfDocumentTemplate apartmentSharingPdfDocumentTemplate;
    private final ApartmentSharingPdfDossierFileGenerationServiceImpl pdfFileGenerationService;


    @Value("${pdf.generation.reattempts}")
    private Integer maxRetries;

    private InputStream inputStreamOfPdfResult(Collection<StorageFile> files, String watermarkText) throws Exception {
        return boPdfDocumentTemplate.render(files.stream()
                        .map(file -> {
                            try {
                                MediaType mediaType = MediaType.valueOf(file.getContentType());
                                if (!mediaType.isPresentIn(Arrays.asList(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.IMAGE_GIF, MediaType.APPLICATION_PDF))) {
                                    throw new UnexpectedException("Unexpected MediaType for storagefile with id [" + file.getId() + "]");
                                }
                                return new FileInputStream(fileStorageService.download(file), mediaType);

                            } catch (Exception e) {
                                log.error("File [" + file.getId() + "] won't be added to the pdf " + e.getMessage() + " - SEntry:" + Sentry.captureException(e));
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()),
                watermarkText
        );
    }

    public void processPdfGenerationFormWatermark(Long documentId) {
        WatermarkDocument document = watermarkDocumentRepository.findById(documentId).orElse(null);

        document.setPdfStatus(FileStatus.IN_PROGRESS);
        watermarkDocumentRepository.saveAndFlush(document);

        try (InputStream inputStream = this.inputStreamOfPdfResult(document.getFiles(), document.getText())) {

            StorageFile pdfFile = StorageFile.builder()
                    .name("dossierfacile-watermark-" + UUID.randomUUID() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .build();
            document.setPdfFile(fileStorageService.upload(inputStream, pdfFile));

            document.setPdfStatus(FileStatus.COMPLETED);
            Collection<StorageFile> files = document.getFiles();
            document.setFiles(null);
            watermarkDocumentRepository.save(document);
            storageFileRepository.deleteAll(files);
        } catch (Exception e) {
            document.setPdfStatus(FileStatus.FAILED);
        } finally {
            watermarkDocumentRepository.save(document);
        }
    }

    private InputStream renderBODocumentPdf(Document document) throws Exception {
        long documentId = document.getId();
        DocumentCategory documentCategory = document.getDocumentCategory();
        DocumentSubCategory documentSubCategory = document.getDocumentSubCategory();

        if (documentSubCategory == DocumentSubCategory.MY_PARENTS
                || documentSubCategory == DocumentSubCategory.LESS_THAN_YEAR
                || documentSubCategory == DocumentSubCategory.OTHER_RESIDENCY
                || ((documentSubCategory == DocumentSubCategory.OTHER_TAX || documentCategory == DocumentCategory.FINANCIAL)
                && document.getNoDocument())) {
            return emptyBOPdfDocumentTemplate.render(document);
        }

        List<File> files = fileRepository.findAllByDocumentId(document.getId());
        if (!CollectionUtils.isEmpty(files)) {
            BOPdfDocumentTemplate pdfDocumentTemplateToUse = (documentCategory == IDENTIFICATION
                    || documentCategory == IDENTIFICATION_LEGAL_PERSON) ? identificationPdfDocumentTemplate : boPdfDocumentTemplate;

            return pdfDocumentTemplateToUse.render(files.stream()
                    .map(file -> {
                        try {
                            MediaType mediaType = MediaType.valueOf(file.getStorageFile().getContentType());
                            if (!mediaType.isPresentIn(Arrays.asList(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.IMAGE_GIF, MediaType.APPLICATION_PDF))) {
                                throw new UnexpectedException("Unexpected MediaType for storagefile with id [" + file.getStorageFile().getId() + "]");
                            }
                            return new FileInputStream(fileStorageService.download(file.getStorageFile()), mediaType);

                        } catch (Exception e) {
                            log.error(e.getMessage() + ". It will not be added to the pdf of document [" + documentCategory.name() + "] with ID [" + documentId + "]");
                            log.error(EXCEPTION + Sentry.captureException(e));
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
            );
        } else {
            log.error("No file were found in the database for generate document [" + documentCategory.name() + "] with ID [" + documentId + "]");
            throw new FileNotFoundException("No file were found in the database for generate document [" + documentCategory.name() + "] with ID [" + documentId + "]");
        }
    }

    @Override
    public StorageFile generateBOPdfDocument(Document document) {

        long documentId = document.getId();
        DocumentCategory documentCategory = document.getDocumentCategory();
        String documentCategoryName = documentCategory.name();
        log.info("Generating PDF for document [" + documentCategoryName + "] with ID [" + documentId + "]");
        StorageFile pdfFile = null;
        try (InputStream documentInputStream = renderBODocumentPdf(document)) {

            pdfFile = StorageFile.builder()
                    .name("dossierfacile-pdf-" + document.getId())
                    .path("dossierfacile_pdf_" + UUID.randomUUID() + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .build();
            StorageFile storageFile = fileStorageService.upload(documentInputStream, pdfFile);

            return storageFile;
        } catch (Exception e) {
            log.error("Unable to generate the watermark pdf file for document " + documentId);
        }
        return null;
    }

    @Override
    public void generateFullDossierPdf(Long apartmentSharingId) {
        ApartmentSharing apartmentSharing = pdfFileGenerationService.initialize(apartmentSharingId);

        if (apartmentSharing != null) {
            StorageFile pdfFile = null;
            try {
                StorageFile storageFile = StorageFile.builder()
                        .name("dossier_" + apartmentSharingId)
                        .path("dossier_pdf_" + UUID.randomUUID() + ".pdf")
                        .contentType(MediaType.APPLICATION_PDF_VALUE)
                        .build();
                InputStream pdfStream = apartmentSharingPdfDocumentTemplate.render(apartmentSharing);
                pdfFile = fileStorageService.upload(pdfStream, storageFile);
                pdfFileGenerationService.complete(apartmentSharingId, pdfFile);
            } catch (Exception e) {
                log.error("Unable to generate the dossierPdfDocument: " + e.getMessage(), e);
                pdfFileGenerationService.fail(apartmentSharingId, pdfFile);
            }
        }
    }
}
