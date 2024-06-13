package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;

public interface DocumentService {
    boolean documentIsUpToDateAt(long timestamp, Long documentId);

    void saveWatermarkFileAt(long executionTimestamp, StorageFile watermarkFile, Long documentId);

    Document getDocument(Long documentId);
}
