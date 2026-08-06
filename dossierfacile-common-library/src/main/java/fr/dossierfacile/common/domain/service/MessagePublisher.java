package fr.dossierfacile.common.domain.service;

import fr.dossierfacile.common.entity.Document;

public interface MessagePublisher {
    void generateFullPdf(Long apartmentSharingId);
    void processFile(Long documentId, Long fileId);
    void sendDocumentForPdfGeneration(Long documentId);
    void sendDocumentForPdfGeneration(Document document);
}
