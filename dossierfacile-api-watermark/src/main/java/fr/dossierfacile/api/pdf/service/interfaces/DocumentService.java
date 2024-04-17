package fr.dossierfacile.api.pdf.service.interfaces;

import fr.dossierfacile.api.pdf.form.DocumentForm;
import fr.dossierfacile.api.pdf.response.UploadFilesResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public interface DocumentService {

    ResponseEntity<UploadFilesResponse> uploadFiles(DocumentForm documentForm);

    ResponseEntity<?> urlPdfDocument(String token);

    void downloadPdfWatermarked(String token, HttpServletResponse response);

    void cleanDocumentsBefore(LocalDateTime date);
}
