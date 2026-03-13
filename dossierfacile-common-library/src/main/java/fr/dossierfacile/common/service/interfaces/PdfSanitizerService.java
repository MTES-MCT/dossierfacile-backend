package fr.dossierfacile.common.service.interfaces;

import java.io.IOException;
import java.io.InputStream;

/**
 * OWASP File Upload — "Run the file through CDR (Content Disarm & Reconstruct) if applicable type (PDF, DOCX, etc...)".
 * <p>
 * Sanitizes PDF files by removing potentially malicious content: JavaScript actions, AcroForms, etc.
 */
public interface PdfSanitizerService {

    /**
     * Sanitizes a PDF input stream by removing interactive/active content.
     *
     * @param input PDF content
     * @return sanitized PDF as a new input stream (caller must close it)
     * @throws IOException if the PDF cannot be read or written
     */
    InputStream sanitize(InputStream input) throws IOException;
}
