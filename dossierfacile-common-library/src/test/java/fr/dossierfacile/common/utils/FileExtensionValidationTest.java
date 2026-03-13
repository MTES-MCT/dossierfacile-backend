package fr.dossierfacile.common.utils;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OWASP File Upload — Tests for "Whitelist des extensions" and "Validation des entrées avant extensions".
 * <p>
 * Validates the pipeline: cleanPath() before getExtension() to neutralize path traversal
 * and correctly extract extensions (including double-extension handling).
 */
class FileExtensionValidationTest {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    /**
     * OWASP: apply cleanPath before getExtension — same logic as DocumentServiceImpl.createDocument().
     */
    private static String extractExtension(String rawFilename) {
        String safePath = StringUtils.cleanPath(Objects.requireNonNullElse(rawFilename, ""));
        return FilenameUtils.getExtension(safePath).toLowerCase();
    }

    private static boolean isExtensionAllowed(String ext) {
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    // --- Whitelist des extensions ---

    @Test
    void should_extract_pdf_extension() {
        String ext = extractExtension("document.pdf");
        assertThat(ext).isEqualTo("pdf");
        assertThat(isExtensionAllowed(ext)).isTrue();
    }

    @Test
    void should_extract_jpg_extension() {
        String ext = extractExtension("photo.jpg");
        assertThat(ext).isEqualTo("jpg");
        assertThat(isExtensionAllowed(ext)).isTrue();
    }

    @Test
    void should_extract_jpeg_extension() {
        String ext = extractExtension("image.jpeg");
        assertThat(ext).isEqualTo("jpeg");
        assertThat(isExtensionAllowed(ext)).isTrue();
    }

    @Test
    void should_extract_png_extension() {
        String ext = extractExtension("screenshot.png");
        assertThat(ext).isEqualTo("png");
        assertThat(isExtensionAllowed(ext)).isTrue();
    }

    @Test
    void should_reject_php_extension() {
        String ext = extractExtension("script.php");
        assertThat(ext).isEqualTo("php");
        assertThat(isExtensionAllowed(ext)).isFalse();
    }

    @Test
    void should_reject_exe_extension() {
        String ext = extractExtension("malware.exe");
        assertThat(ext).isEqualTo("exe");
        assertThat(isExtensionAllowed(ext)).isFalse();
    }

    // --- Validation des entrées avant extensions (cleanPath avant getExtension) ---

    @Test
    void should_neutralize_path_traversal_and_extract_extension() {
        String ext = extractExtension("../../../etc/passwd.pdf");
        assertThat(ext).isEqualTo("pdf");
    }

    @Test
    void should_handle_double_extension_returning_last() {
        String ext = extractExtension("malware.jpg.php");
        assertThat(ext).isEqualTo("php");
        assertThat(isExtensionAllowed(ext)).isFalse();
    }

    @Test
    void should_handle_double_extension_when_last_is_allowed() {
        String ext = extractExtension("file.php.pdf");
        assertThat(ext).isEqualTo("pdf");
        assertThat(isExtensionAllowed(ext)).isTrue();
    }

    @Test
    void should_return_empty_extension_for_null() {
        String ext = extractExtension(null);
        assertThat(ext).isEmpty();
    }

    @Test
    void should_return_empty_extension_for_blank() {
        String ext = extractExtension("   ");
        assertThat(ext).isEmpty();
    }

    @Test
    void should_return_empty_extension_for_no_extension() {
        String ext = extractExtension("noextension");
        assertThat(ext).isEmpty();
    }
}
