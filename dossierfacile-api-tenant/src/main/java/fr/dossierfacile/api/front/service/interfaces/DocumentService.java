package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.tenant.DocumentAnalysisStatusResponse;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentService {
    void delete(Long id, Tenant tenant);

    void resetValidatedOrInProgressDocumentsAccordingCategories(List<Document> documentList, List<DocumentCategory> categoriesToChange);

    void changeDocumentStatus(Document document, DocumentStatus toProcess);

    void addFile(MultipartFile multipartFile, String detectedMimeType, Document document) throws IOException;

    void delete(Document document);

    Tenant resolveDocumentTenant(Document document);

    void markDocumentAsEdited(Document document);

    Document getAuthorizedDocument(String documentName, Tenant tenant);

    DocumentAnalysisStatusResponse getDocumentAnalysisStatus(Long documentId, Tenant tenant);
}
