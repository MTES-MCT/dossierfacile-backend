package fr.dossierfacile.common.utils;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

    // --- Whitelist des extensions : allowed types ---

    @ParameterizedTest(name = "{0} → extension \"{1}\" allowed")
    @CsvSource({
            "document.pdf,   pdf",
            "photo.jpg,      jpg",
            "image.jpeg,     jpeg",
            "screenshot.png, png",
    })
    void should_extract_allowed_extension(String filename, String expectedExt) {
        String ext = extractExtension(filename.trim());
        assertThat(ext).isEqualTo(expectedExt.trim()).isIn(ALLOWED_EXTENSIONS);
    }

    // --- Whitelist des extensions : rejected types ---

    @ParameterizedTest(name = "{0} → extension \"{1}\" rejected")
    @CsvSource({
            "script.php,  php",
            "malware.exe, exe",
    })
    void should_reject_disallowed_extension(String filename, String expectedExt) {
        String ext = extractExtension(filename.trim());
        assertThat(ext).isEqualTo(expectedExt.trim()).isNotIn(ALLOWED_EXTENSIONS);
    }

    // --- Validation des entrées avant extensions ---

    @ParameterizedTest(name = "{0} → last extension \"{1}\", allowed={2}")
    @CsvSource({
            "../../../etc/passwd.pdf, pdf,  true",
            "malware.jpg.php,         php,  false",
            "file.php.pdf,            pdf,  true",
    })
    void should_extract_extension_after_input_sanitization(String filename, String expectedExt, boolean expectedAllowed) {
        String ext = extractExtension(filename.trim());
        assertThat(ext).isEqualTo(expectedExt.trim())
                .satisfies(e -> assertThat(ALLOWED_EXTENSIONS.contains(e)).isEqualTo(expectedAllowed));
    }

    // --- Edge cases: no extension ---

    @ParameterizedTest(name = "blank or empty filename → empty extension")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "noextension"})
    void should_return_empty_extension_for_no_extension(String filename) {
        assertThat(extractExtension(filename)).isEmpty();
    }
}
