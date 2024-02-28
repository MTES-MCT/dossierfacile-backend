package fr.dossierfacile.api.pdf.controller;

import fr.dossierfacile.api.pdf.form.DocumentForm;
import fr.dossierfacile.api.pdf.response.UploadFilesResponse;
import fr.dossierfacile.api.pdf.service.interfaces.DocumentService;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletResponse;
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

    @ApiOperation("1.Envoyer une liste de fichiers à filigraner et le texte du filigrane / recuperation d'un token")
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadFilesResponse> uploadFiles(@Validated DocumentForm documentForm) {
        return documentService.uploadFiles(documentForm);
    }

    @ApiOperation("2.Obtenir l'url du document à partir du token (status du processus de traitement)")
    @GetMapping(value = "/url/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> urlPdf(@PathVariable("token") String documentToken) {
        return documentService.urlPdfDocument(documentToken);
    }

    @ApiOperation("3.Télécharger le document filigrané à partir de l'url")
    @GetMapping(value = "/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadPdfWatermarked(@PathVariable("token") String documentToken, HttpServletResponse response) {
        documentService.downloadPdfWatermarked(documentToken, response);
    }
}
