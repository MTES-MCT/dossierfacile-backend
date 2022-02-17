package fr.dossierfacile.api.pdfgenerator.service.interfaces;

public interface DocumentPdfGenerationLogService {
    void deactivateNewerMessages(Long documentId, Long logId);
    void updateDocumentPdfGenerationLog(Long logId);
}
