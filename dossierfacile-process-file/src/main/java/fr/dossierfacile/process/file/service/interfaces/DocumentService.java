package fr.dossierfacile.process.file.service.interfaces;

import fr.dossierfacile.common.entity.Document;

public interface DocumentService {
    boolean documentIsUpToDateAt(Long timestamp, Long documentId);

    Document getDocument(Long documentId);
}
