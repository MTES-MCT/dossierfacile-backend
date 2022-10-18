package fr.dossierfacile.api.pdf.service.interfaces;

import fr.dossierfacile.api.pdf.form.DocumentForm;
import fr.dossierfacile.api.pdf.response.DocumentUrlResponse;
import fr.dossierfacile.api.pdf.response.UploadFilesResponse;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface DocumentService {

    ResponseEntity<UploadFilesResponse> uploadFiles(DocumentForm documentForm);

    ResponseEntity<DocumentUrlResponse> urlPdfDocument(String token);

    void downloadPdfWatermarked(String token, HttpServletResponse response);

    void cleanOldDocuments();
}
