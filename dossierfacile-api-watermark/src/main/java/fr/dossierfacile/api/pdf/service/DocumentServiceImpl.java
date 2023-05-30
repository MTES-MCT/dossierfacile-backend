package fr.dossierfacile.api.pdf.service;

import com.google.common.base.Strings;
import fr.dossierfacile.api.pdf.amqp.Producer;
import fr.dossierfacile.api.pdf.exceptions.DocumentBadRequestException;
import fr.dossierfacile.api.pdf.exceptions.DocumentNotFoundException;
import fr.dossierfacile.api.pdf.exceptions.DocumentTokenNotFoundException;
import fr.dossierfacile.api.pdf.exceptions.ExpectationFailedException;
import fr.dossierfacile.api.pdf.exceptions.InProgressException;
import fr.dossierfacile.api.pdf.form.DocumentForm;
import fr.dossierfacile.api.pdf.repository.DocumentRepository;
import fr.dossierfacile.api.pdf.repository.DocumentTokenRepository;
import fr.dossierfacile.api.pdf.repository.FileRepository;
import fr.dossierfacile.api.pdf.response.DocumentUrlResponse;
import fr.dossierfacile.api.pdf.response.UploadFilesResponse;
import fr.dossierfacile.api.pdf.service.interfaces.DocumentService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import fr.dossierfacile.common.entity.DocumentToken;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.exceptions.OvhConnectionFailedException;
import fr.dossierfacile.common.repository.DocumentPdfGenerationLogRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private static final int RETENTION_DAYS = 10;
    private static final String DOCUMENT_NOT_EXIST = "The document does not exist";
    private final DocumentRepository documentRepository;
    private final DocumentTokenRepository documentTokenRepository;
    private final FileRepository fileRepository;
    private final DocumentHelperService documentHelperService;
    private final DocumentPdfGenerationLogRepository documentPdfGenerationLogRepository;
    private final Producer producer;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ResponseEntity<UploadFilesResponse> uploadFiles(DocumentForm documentForm) {
        if (documentForm.getFiles().stream().allMatch(MultipartFile::isEmpty)) {
            throw new DocumentBadRequestException("you must add some file");
        }

        Document document = createDocument(documentForm);
        producer.generatePdf(document.getId(),
                documentPdfGenerationLogRepository.save(DocumentPdfGenerationLog.builder()
                        .documentId(document.getId())
                        .build()).getId());

        UploadFilesResponse uploadFilesResponse = UploadFilesResponse.builder()
                .token(createDocumentToken(document).getToken())
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
    public ResponseEntity<DocumentUrlResponse> urlPdfDocument(String token) {
        DocumentToken documentToken = documentTokenRepository.findFirstByToken(token)
                .orElseThrow(() -> new DocumentTokenNotFoundException(token));

        Long documentId = documentToken.getDocument().getId();
        Document document = documentRepository.findFirstByIdAndDocumentCategory(documentId, DocumentCategory.NULL)
                .orElseThrow(() -> new DocumentNotFoundException(documentId, DocumentCategory.NULL.name()));

        if (Strings.isNullOrEmpty(document.getName())) {
            if (document.getProcessingStartTime() == null) {
                // was not sent to generate its PDF
                throw new ExpectationFailedException("Document with ID [" + documentId + "] was not sent to generate its PDF previously for some reason");
            } else if (document.getProcessingStartTime() != null && document.getProcessingEndTime() != null) {
                // should be updated, it is not possible startTime and endTime have values and pdf is not yet generated
                throw new ExpectationFailedException("Document with ID [" + documentId + "] not yet generated but its startTime and endTime are different from NULL");
            } else if (document.getProcessingStartTime() != null && document.getProcessingEndTime() == null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(document.getProcessingStartTime().plusHours(1))) {
                    throw new ExpectationFailedException("PDF generation FAILED after 1 hour for Document with ID [" + documentId + "]");
                }
                throw new InProgressException("PDF generation still IN PROGRESS for Document with ID [" + documentId + "]");
            }
            throw new ExpectationFailedException("PDF generation FAILED for unknown reason for Document with ID [" + documentId + "]");
        }

        DocumentUrlResponse documentUrlResponse = DocumentUrlResponse.builder()
                .url(document.getName())
                .build();

        return new ResponseEntity<>(documentUrlResponse, HttpStatus.OK);
    }

    @Override
    @Transactional
    public void downloadPdfWatermarked(String token, HttpServletResponse response) {
        DocumentToken documentToken = documentTokenRepository.findFirstByToken(token)
                .orElseThrow(() -> new DocumentTokenNotFoundException(token));
        Document document = documentToken.getDocument();

        if (Strings.isNullOrEmpty(document.getName())) {
            log.error("document pdf path is null or empty");
            response.setStatus(404);
            return;
        }

        try (InputStream in = fileStorageService.download(document.getName(), null)) {
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            IOUtils.copy(in, response.getOutputStream());
            List<String> fileList = deleteDataFromDB(documentToken, document);
            deleteFilesFromStorage(fileList, document.getId());
        } catch (FileNotFoundException e) {
            log.error(DOCUMENT_NOT_EXIST);
            response.setStatus(404);
        } catch (IOException e) {
            log.error("Cannot download document", e);
            response.setStatus(404);
        }
    }

    @Override
    @Transactional
    public void cleanOldDocuments() {
        LocalDateTime date = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<DocumentToken> documentTokens = documentTokenRepository.findAllByCreationDateBefore(date);
        for (DocumentToken documentToken : documentTokens) {
            List<String> fileList = deleteDataFromDB(documentToken, documentToken.getDocument());
            deleteFilesFromStorage(fileList, documentToken.getDocument().getId());
        }
    }

    Document createDocument(DocumentForm documentForm) {
        //a new document is created.
        Document document = Document.builder()
                .documentCategory(DocumentCategory.NULL)
                .build();
        documentRepository.save(document);

        try {
            documentForm.getFiles().stream()
                    .filter(f -> !f.isEmpty())
                    .forEach(multipartFile -> {
                        try {
                            documentHelperService.addFile(multipartFile, document);
                        } catch (Exception e) {
                            log.error("Unable to add file to document " + multipartFile.getOriginalFilename(), e);
                        }
                    });
        } catch (OvhConnectionFailedException e) {
            log.error(e.getMessage());
            log.error("Deleting document with ID [" + document.getId() + "] after error FOUND saving its files");
            documentRepository.delete(document);
            throw e;
        }

        return document;
    }

    private DocumentToken createDocumentToken(Document document) {
        DocumentToken documentToken = documentTokenRepository.findFirstByDocumentId(document.getId())
                .orElse(DocumentToken.builder()
                        .creationDate(LocalDateTime.now())
                        .token(UUID.randomUUID().toString())
                        .document(document)
                        .build());
        return documentTokenRepository.save(documentToken);
    }

    private List<String> deleteDataFromDB(DocumentToken documentToken, Document document) {
        List<File> fileList = document.getFiles();
        List<String> result = new ArrayList<>();
        if (fileList != null && !fileList.isEmpty()) {
            fileList.stream().forEach(df -> fileStorageService.delete(df.getStorageFile()));
        }
        documentTokenRepository.delete(documentToken);
        documentRepository.delete(document);
        return result;
    }

    private void deleteFilesFromStorage(List<String> fileList, Long documentId) {
        log.info("Removing files from storage for document with ID [" + documentId + "]");
        fileStorageService.delete(fileList);
    }
}
