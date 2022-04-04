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

    /**
     * Gets DocumentInputStream of a specified file from OVH storage.
     *
     * @param filePath name of object in storage
     * @return file's content inputStream
     */
    InputStream getFileInputStream(String filePath);
}
