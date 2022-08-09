package fr.dossierfacile.api.pdf.controller;

import fr.dossierfacile.api.pdf.form.DocumentForm;
import fr.dossierfacile.api.pdf.response.DocumentUrlResponse;
import fr.dossierfacile.api.pdf.response.UploadFilesResponse;
import fr.dossierfacile.api.pdf.service.interfaces.DocumentService;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@CrossOrigin
@RequestMapping("/api/document")
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadFilesResponse> uploadFiles(@Validated DocumentForm documentForm) {
        return documentService.uploadFiles(documentForm);
    }

    @GetMapping(value = "/url/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentUrlResponse> urlPdf(@PathVariable("token") String documentToken) {
        return documentService.urlPdfDocument(documentToken);
    }

    @GetMapping(value = "/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadPdfWatermarked(@PathVariable("token") String documentToken, HttpServletResponse response) {
        documentService.downloadPdfWatermarked(documentToken, response);
    }
}
