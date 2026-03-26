package fr.dossierfacile.common.service;

import fr.dossierfacile.common.service.interfaces.PdfSanitizerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * OWASP File Upload — "Run the file through CDR (Content Disarm & Reconstruct) if applicable type (PDF, DOCX, etc...)".
 * <p>
 * Implements CDR for PDF via PDFBox: removes JavaScript open actions and AcroForm/XFA content
 * to mitigate risks from malicious embedded code.
 * <p>
 * Uses temp files instead of in-memory byte arrays to avoid excessive heap usage on large uploads.
 * PDFBox stream cache is configured with a RAM limit; overflow spills to temp files on disk.
 */
@Slf4j
@Service
public class PdfSanitizerServiceImpl implements PdfSanitizerService {

    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");
    private static final boolean POSIX_SUPPORTED =
            FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    private final long maxMainMemoryBytes;

    public PdfSanitizerServiceImpl(
        @Value("${pdfbox.sanitizer.max-main-memory-bytes:16777216}") long maxMainMemoryBytes) {
        this.maxMainMemoryBytes = maxMainMemoryBytes;
    }

    @Override
    public InputStream sanitize(InputStream input) throws IOException {
        Path tempInput = createTempPdf("pdf-sanitize-in-");
        Path tempOutput = createTempPdf("pdf-sanitize-out-");
        try {
            try (InputStream in = input) {
                Files.copy(in, tempInput, StandardCopyOption.REPLACE_EXISTING);
            }

            var streamCache = MemoryUsageSetting.setupMixed(maxMainMemoryBytes).streamCache;
            try (PDDocument doc = Loader.loadPDF(
                    new RandomAccessReadBufferedFile(tempInput.toFile()), streamCache)) {

                var catalog = doc.getDocumentCatalog();
                catalog.setOpenAction(null);
                catalog.setAcroForm(null);

                PDDocumentNameDictionary names = catalog.getNames();
                if (names != null && names.getJavaScript() != null) {
                    names.setJavascript(null);
                }

                try (OutputStream fos = Files.newOutputStream(tempOutput)) {
                    doc.save(fos);
                }
            }

            Files.deleteIfExists(tempInput);

            return new FileInputStream(tempOutput.toFile()) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        Files.deleteIfExists(tempOutput);
                    }
                }
            };
        } catch (IOException e) {
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempOutput);
            throw e;
        }
    }

    private static Path createTempPdf(String prefix) throws IOException {
        Path path;
        if (POSIX_SUPPORTED) {
            path = Files.createTempFile(prefix, ".pdf", PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        } else {
            // On Windows, posix file attribute view is not supported, so we use the default file creation.
            // Production environment is on Linux, so we can safely use the default file creation for dev environment on Windows.
            path = Files.createTempFile(prefix, ".pdf");
        }
        return path;
    }
}
