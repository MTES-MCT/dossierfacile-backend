package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;

public interface PdfSignatureService {

    void signAndSave(PDDocument document, ByteArrayOutputStream baos) throws Exception;

    /**
     * Signs and saves like {@link #signAndSave(PDDocument, ByteArrayOutputStream)}, and keeps the title and the
     * description of the document. A tagged PDF needs them, because readers display the title instead of the file name.
     */
    void signAndSaveKeepingDescription(PDDocument document, ByteArrayOutputStream baos) throws Exception;
}
