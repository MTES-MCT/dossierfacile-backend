package fr.dossierfacile.common.service;

import fr.dossierfacile.common.service.interfaces.PdfSanitizerService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * OWASP File Upload — "Run the file through CDR (Content Disarm & Reconstruct) if applicable type (PDF, DOCX, etc...)".
 * <p>
 * Implements CDR for PDF via PDFBox: removes JavaScript open actions and AcroForm/XFA content
 * to mitigate risks from malicious embedded code.
 */
@Service
public class PdfSanitizerServiceImpl implements PdfSanitizerService {

    @Override
    public InputStream sanitize(InputStream input) throws IOException {
        byte[] bytes = input.readAllBytes();

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            var catalog = doc.getDocumentCatalog();

            // Remove JavaScript/actions that execute on document open
            catalog.setOpenAction(null);

            // Remove AcroForm (may contain XFA, JavaScript, submit actions)
            catalog.setAcroForm(null);
            
            PDDocumentNameDictionary names = catalog.getNames();
            if (names != null && names.getJavaScript() != null) {
                names.setJavascript(null);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
