package fr.dossierfacile.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileUtilityTest {

    @Test
    void sanitizeFilename_should_return_file_for_null() {
        assertThat(FileUtility.sanitizeFilename(null)).isEqualTo("file");
    }

    @Test
    void sanitizeFilename_should_return_file_for_blank() {
        assertThat(FileUtility.sanitizeFilename("   ")).isEqualTo("file");
    }

    @Test
    void sanitizeFilename_should_return_file_when_result_empty() {
        assertThat(FileUtility.sanitizeFilename("@@@")).isEqualTo("file");
    }

    @Test
    void sanitizeFilename_should_neutralize_path_traversal() {
        assertThat(FileUtility.sanitizeFilename("../../../etc/passwd"))
                .doesNotContain("/")
                .isEqualTo("......etcpasswd");
    }

    @Test
    void sanitizeFilename_should_preserve_safe_chars() {
        assertThat(FileUtility.sanitizeFilename("document_123.pdf")).isEqualTo("document_123.pdf");
    }

    @Test
    void sanitizeFilename_should_remove_accents() {
        assertThat(FileUtility.sanitizeFilename("éàùç")).isEqualTo("eauc");
    }

    @Test
    void sanitizeFilename_should_replace_spaces_with_underscores() {
        assertThat(FileUtility.sanitizeFilename("my file.pdf")).isEqualTo("my_file.pdf");
    }

    // --- OWASP: Limite longueur / caractères du nom ---

    @Test
    void sanitizeAndTruncateFilename_should_preserve_filename_under_limit() {
        String result = FileUtility.sanitizeAndTruncateFilename("document_123.pdf", 200);
        assertThat(result).isEqualTo("document_123.pdf");
    }

    @Test
    void sanitizeAndTruncateFilename_should_truncate_base_and_preserve_extension() {
        assertThat(FileUtility.sanitizeAndTruncateFilename("a".repeat(250) + ".pdf", 200))
                .endsWith(".pdf")
                .hasSizeLessThanOrEqualTo(205);
    }

    @Test
    void sanitizeAndTruncateFilename_should_use_default_max_200() {
        assertThat(FileUtility.sanitizeAndTruncateFilename("x".repeat(300) + ".pdf"))
                .endsWith(".pdf")
                .hasSizeLessThanOrEqualTo(205);
    }

    @Test
    void sanitizeAndTruncateFilename_should_handle_empty_extension() {
        assertThat(FileUtility.sanitizeAndTruncateFilename("a".repeat(300), 50))
                .hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void sanitizeAndTruncateFilename_should_sanitize_before_truncating() {
        assertThat(FileUtility.sanitizeAndTruncateFilename("../../file.pdf", 200))
                .doesNotContain("/")
                .endsWith(".pdf");
    }

    @Test
    void sanitizeAndTruncateFilename_should_return_file_for_invalid_input() {
        String result = FileUtility.sanitizeAndTruncateFilename("@@@", 200);
        assertThat(result).isEqualTo("file");
    }
}
