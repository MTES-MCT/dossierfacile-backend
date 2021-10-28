package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;

import java.util.List;

public interface DocumentService {
    void delete(Long id, Tenant tenant);

    void initializeFieldsToProcessPdfGeneration(Document document);

    void initializeFieldsToProcessPdfGeneration(long documentId);

    void resetValidatedDocumentsStatusToToProcess(Tenant tenant);

    void resetValidatedAndDeniedDocumentsStatusToToProcess(List<Document> documentList);
}
