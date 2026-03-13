package fr.dossierfacile.common.service;

import fr.dossierfacile.common.model.ValidatedFile;
import fr.dossierfacile.common.service.interfaces.MimeTypeDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Converts a raw list of uploaded files into {@link ValidatedFile} instances by detecting
 * the real MIME type of each file once (via Tika magic bytes).
 * <p>
 * Must be called after bean-validation ({@code @Validated}) so that the whitelist check
 * has already rejected disallowed types before detection occurs.
 */
@Service
@RequiredArgsConstructor
public class FileUploadPreprocessor {

    /**
     * OWASP File Upload — "List allowed extensions" / "Validate the file type, don't trust the Content-Type header".
     * Shared whitelist used by all upload entry points.
     */
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/heif"
    );

    private final MimeTypeDetectionService mimeTypeDetectionService;

    /**
     * Filters out empty files, then detects the MIME type of each remaining file.
     *
     * @param files raw list from the form
     * @return list of (file, detectedMimeType) pairs, never null
     * @throws IOException if a file's content cannot be read during detection
     */
    public List<ValidatedFile> prepareValidatedFiles(List<MultipartFile> files) throws IOException {
        List<ValidatedFile> result = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                result.add(new ValidatedFile(f, mimeTypeDetectionService.detect(f)));
            }
        }
        return result;
    }
}
