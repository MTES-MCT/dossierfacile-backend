package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.Document;

import java.io.InputStream;

public interface DownloadService {
    /**
     * Gets DocumentInputStream of a specified document from OVH storage.
     *
     * @param document input's document
     * @return document's content inputStream
     */
    InputStream getDocumentInputStream(Document document);

}
