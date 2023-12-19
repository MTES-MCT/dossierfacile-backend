package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;

import java.time.LocalDateTime;

public interface DocumentService {
    boolean documentIsUpToDateAt(Long timestamp, Long documentId);

    void saveWatermarkFileAt(LocalDateTime executionDateTime, StorageFile watermarkFile, Long documentId);

    Document getDocument(Long documentId);
}
