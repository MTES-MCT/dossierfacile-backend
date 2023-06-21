package fr.dossierfacile.api.pdf.service.interfaces;

import fr.dossierfacile.api.pdf.form.DocumentForm;
import fr.dossierfacile.api.pdf.response.UploadFilesResponse;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;

public interface DocumentService {

    ResponseEntity<UploadFilesResponse> uploadFiles(DocumentForm documentForm);

    ResponseEntity<?> urlPdfDocument(String token);

    void downloadPdfWatermarked(String token, HttpServletResponse response);

    void cleanOldDocuments();
}
