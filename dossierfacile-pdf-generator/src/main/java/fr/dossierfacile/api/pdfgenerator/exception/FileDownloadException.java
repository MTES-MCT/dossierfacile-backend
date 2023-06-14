package fr.dossierfacile.api.pdfgenerator.exception;

import fr.dossierfacile.common.entity.Document;

public class FileDownloadException extends RuntimeException {
    public FileDownloadException(String filename) {
        super("Could not download file [ " + filename + "]");
    }

    public FileDownloadException(Document document) {
        super("Could not download document [ " + document.getDocumentCategory().name() + " ," + document.getId());
    }
}
