package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.Document;

import java.io.IOException;

public interface PdfGeneratorService {
    void processPdfGenerationOfDocument(Long documentId, Long logId);

    void processPdfGenerationFormWatermark(Long watermarkDocumentId);

    void lockDocument(Document document, String lockedBy);

    void generateBOPdfDocument(Document document, Long logId) throws Exception;

    void generateFullDossierPdf(Long apartmentSharingId) throws IOException;
}
