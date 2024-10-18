package fr.dossierfacile.api.pdf.service;

import fr.dossierfacile.api.pdf.amqp.Producer;
import fr.dossierfacile.api.pdf.exceptions.DocumentBadRequestException;
import fr.dossierfacile.api.pdf.exceptions.DocumentTokenNotFoundException;
import fr.dossierfacile.api.pdf.form.DocumentForm;
import fr.dossierfacile.api.pdf.response.DocumentUrlResponse;
import fr.dossierfacile.api.pdf.response.UploadFilesResponse;
import fr.dossierfacile.api.pdf.service.interfaces.DocumentService;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.WatermarkDocument;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.repository.WatermarkDocumentRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.EncryptionKeyService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private static final String DOCUMENT_NOT_EXIST = "The document does not exist";
    private final Producer producer;
    private final FileStorageService fileStorageService;
    private final EncryptionKeyService encryptionKeyService;
    private final WatermarkDocumentRepository watermarkDocumentRepository;
    private final DocumentHelperService documentHelperService;

    @Override
    @Transactional
    public ResponseEntity<UploadFilesResponse> uploadFiles(DocumentForm documentForm) {
        if (documentForm.getFiles().stream().allMatch(MultipartFile::isEmpty)) {
            throw new DocumentBadRequestException("you must add some file");
        }

        WatermarkDocument document = createDocument(documentForm);
        producer.generatePdf(document.getId(), null);

        UploadFilesResponse uploadFilesResponse = UploadFilesResponse.builder()
                .token(document.getToken())
                .build();

        return new ResponseEntity<>(uploadFilesResponse, HttpStatus.OK);
    }

    /**
     * It will return an instance of ResponseEntity<DocumentUrlResponse> containing the url of the PDF or a message
     * <p>
     * Possible HTTP_STATUS :
     * - OK
     * - PROCESSING
     * - NOT_FOUND
     * - EXPECTATION_FAILED
     *
     * @param token : String
     * @return { "url" : "<name_of_pdf>.pdf" } -> Example: { "url" : "as9d8a89sa7sd87asd87a8sd8a7d8as.pdf" }
     */
    @Override
    public ResponseEntity<?> urlPdfDocument(String token) {
        WatermarkDocument document = watermarkDocumentRepository.findOneByToken(token)
                .orElseThrow(() -> new DocumentTokenNotFoundException(token));

        switch (document.getPdfStatus()) {
            case IN_PROGRESS:
                return ResponseEntity.status(HttpStatus.CONFLICT).body("PDF generation still IN PROGRESS for Document with ID [" + "]");
            case FAILED:
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Document with ID [" + "] not generated due to error");
            case DELETED:
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Document with ID [" + "] has been deleted");
            case NONE:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document with ID [" + "] not generated");
            default: // continue
        }

        DocumentUrlResponse documentUrlResponse = DocumentUrlResponse.builder()
                .url(document.getToken())
                .build();

        return new ResponseEntity<>(documentUrlResponse, HttpStatus.OK);
    }

    @Override
    @Transactional
    public void downloadPdfWatermarked(String token, HttpServletResponse response) {
        WatermarkDocument document = watermarkDocumentRepository.findOneByToken(token)
                .orElseThrow(() -> new DocumentTokenNotFoundException(token));

        if (document.getPdfFile() == null) {
            log.error("document pdf path is null or empty");
            response.setStatus(404);
            return;
        }

        try (InputStream in = fileStorageService.download(document.getPdfFile())) {
            response.setContentType(document.getPdfFile().getContentType());
            IOUtils.copy(in, response.getOutputStream());
            cleanData(document);
        } catch (FileNotFoundException e) {
            log.error(DOCUMENT_NOT_EXIST);
            response.setStatus(404);
        } catch (IOException e) {
            log.error("Cannot download document", e);
            response.setStatus(404);
        }
    }

    private void cleanData(WatermarkDocument document) {
        document.setFiles(null);
        if (document.getPdfStatus() == FileStatus.COMPLETED || document.getPdfStatus() == FileStatus.IN_PROGRESS) {
            document.setPdfStatus(FileStatus.DELETED);
        }
        StorageFile pdfFile = document.getPdfFile();
        document.setPdfFile(null);
        document.setPdfStatus(FileStatus.DELETED);
        watermarkDocumentRepository.save(document);
        fileStorageService.delete(pdfFile);
    }

    @Override
    @Transactional
    public void cleanDocumentsBefore(LocalDateTime date) {
        List<WatermarkDocument> docs = watermarkDocumentRepository.findAllByPdfStatusNotAndCreatedDateBefore(FileStatus.DELETED, date);
        docs.stream().forEach(doc -> cleanData(doc));
    }

    private WatermarkDocument createDocument(DocumentForm documentForm) {
        List<StorageFile> files = documentForm.getFiles().stream()
                .filter(f -> !f.isEmpty())
                .map(multipartFile -> {
                    StorageFile file = StorageFile.builder()
                            .name(multipartFile.getOriginalFilename())
                            .contentType(multipartFile.getContentType())
                            .size(multipartFile.getSize())
                            .encryptionKey(encryptionKeyService.getCurrentKey())
                            .status(FileStorageStatus.TEMPORARY)
                            .build();

                    try {
                        if ("image/heif".equals(multipartFile.getContentType())) {
                            file.setName(multipartFile.getOriginalFilename().replaceAll("(?i)\\.heic$", "") + ".jpg");
                            file.setContentType("image/jpeg");

                            InputStream jpgInputStream = documentHelperService.convertHeicToJpg(multipartFile.getInputStream());
                            if (jpgInputStream != null) {
                                return fileStorageService.upload(jpgInputStream, file);
                            } else {
                                throw new IOException("Image could not be saved");
                            }
                        } else {
                            return fileStorageService.upload(multipartFile.getInputStream(), file);
                        }

                    } catch (IOException e) {
                        log.error("Error on file save", e);
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        return watermarkDocumentRepository.save(WatermarkDocument.builder()
                .token(UUID.randomUUID().toString())
                .text(documentForm.getWatermark())
                .files(files)
                .pdfStatus(FileStatus.NONE)
                .build());
    }
}