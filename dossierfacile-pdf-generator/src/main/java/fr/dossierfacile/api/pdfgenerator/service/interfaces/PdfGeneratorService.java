package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.Document;

public interface PdfGeneratorService {
    void processPdfGenerationOfDocument(Long documentId);

    void lockDocument(Document document, String lockedBy);

    void unLockDocumentSuccessfulGeneration(Document document);

    void unLockDocumentFailedGeneration(Document document);

    void generatePdf(Document document);
}
