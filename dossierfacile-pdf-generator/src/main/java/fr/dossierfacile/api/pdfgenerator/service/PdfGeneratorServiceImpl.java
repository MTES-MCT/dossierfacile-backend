package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.model.FileInputStream;
import fr.dossierfacile.api.pdfgenerator.repository.DocumentRepository;
import fr.dossierfacile.api.pdfgenerator.repository.FileRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfGeneratorService;
import fr.dossierfacile.api.pdfgenerator.service.templates.ApartmentSharingPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.BOIdentificationPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.BOPdfDocumentTemplate;
import fr.dossierfacile.api.pdfgenerator.service.templates.EmptyBOPdfDocumentTemplate;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.repository.WatermarkDocumentRepository;
import fr.dossierfacile.common.service.interfaces.EncryptionKeyService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.rmi.UnexpectedException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static fr.dossierfacile.common.enums.DocumentCategory.IDENTIFICATION;
import static fr.dossierfacile.common.enums.DocumentCategory.IDENTIFICATION_LEGAL_PERSON;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

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
    private final EncryptionKeyService encryptionKeyService;
    private final DocumentRepository documentRepository;


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
                                log.error("File [" + file.getId() + "] won't be added to the pdf " + e.getMessage());
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
                    .path(StorageFile.getWatermarkPdfPath())
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .bucket(S3Bucket.FILIGRANE)
                    .encryptionKey(encryptionKeyService.getCurrentKey())
                    .provider(ObjectStorageProvider.S3)
                    .status(FileStorageStatus.TEMPORARY)
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
                            throw new RuntimeException("Unable to get a file input stream on fileId :" + file.getId() + " docId:" + documentId);
                        }
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
    @Transactional
    public StorageFile generateBOPdfDocument(Long documentId) throws FileNotFoundException {
        Document document = documentRepository.findById(documentId).orElseThrow(() -> new FileNotFoundException(documentId.toString()));
        DocumentCategory documentCategory = document.getDocumentCategory();
        String documentCategoryName = documentCategory.name();
        log.info("Generating PDF for document [" + documentCategoryName + "] with ID [" + documentId + "]");
        StorageFile pdfFile = null;
        try (InputStream documentInputStream = renderBODocumentPdf(document)) {

            pdfFile = StorageFile.builder()
                    .name("dossierfacile-pdf-" + document.getId() + ".pdf")
                    .path(String.format("%s/%s", document.getDocumentS3PrefixPath(), UUID.randomUUID()))
                    .bucket(S3Bucket.WATERMARK_DOC)
                    .encryptionKey(encryptionKeyService.getCurrentKey())
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .build();
            return fileStorageService.upload(documentInputStream, pdfFile);
        } catch (Exception e) {
            log.error("Unable to generate the watermark pdf file for document " + documentId);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void generateFullDossierPdf(Long apartmentSharingId) {
        ApartmentSharing apartmentSharing = pdfFileGenerationService.initialize(apartmentSharingId);

        if (apartmentSharing != null) {
            StorageFile pdfFile = null;
            try {
                StorageFile storageFile = StorageFile.builder()
                        .name("dossier_" + apartmentSharingId)
                        .bucket(S3Bucket.FULL_PDF)
                        .encryptionKey(encryptionKeyService.getCurrentKey())
                        .path(String.format("Apartment_%s/%s", apartmentSharing.getId(), UUID.randomUUID()))
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
