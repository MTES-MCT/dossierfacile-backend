package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.Document;

public interface PdfGeneratorService {
    void processPdfGenerationOfDocument(Long documentId, Long logId);

    void lockDocument(Document document, String lockedBy);

    void generatePdf(Document document, Long logId);
}
