package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface PdfSignatureService {

    void signAndSave(PDDocument document, ByteArrayOutputStream baos) throws Exception;

    /**
     * Signs and saves like {@link #signAndSave(PDDocument, ByteArrayOutputStream)}, and keeps the title, the
     * subject, the author and the producer of the document. A tagged PDF needs them, because readers display the title
     * instead of the file name. They must also match the XMP metadata, which the signature does not change.
     */
    void signAndSaveKeepingMetadata(PDDocument document, ByteArrayOutputStream baos) throws IOException;
}
