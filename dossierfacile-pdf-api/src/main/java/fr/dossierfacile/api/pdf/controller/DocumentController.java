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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/document")
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadFilesResponse> uploadFiles(@Validated DocumentForm documentForm) {
        return documentService.uploadFiles(documentForm);
    }

    @GetMapping(value = "/url/{documentToken}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentUrlResponse> urlPdf(@PathVariable("documentToken") String documentToken) {
        return documentService.urlPdfDocument(documentToken);
    }

    @GetMapping(value = "/{documentToken}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadPdfWatermarked(@PathVariable("documentToken") String documentToken, HttpServletResponse response) {
        documentService.downloadPdfWatermarked(documentToken, response);
    }
}
