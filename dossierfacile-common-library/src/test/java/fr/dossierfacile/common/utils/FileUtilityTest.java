package fr.dossierfacile.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FileUtilityTest {

    @ParameterizedTest
    @MethodSource("invalidInputsThatReturnFile")
    void sanitizeFilename_should_return_file_for_invalid_inputs(String input) {
        assertThat(FileUtility.sanitizeFilename(input)).isEqualTo("file");
    }

    static Stream<String> invalidInputsThatReturnFile() {
        return Stream.of(null, "   ", "@@@");
    }

    @Test
    void sanitizeFilename_should_neutralize_path_traversal() {
        assertThat(FileUtility.sanitizeFilename("../../../etc/passwd"))
                .doesNotContain("/")
                .isEqualTo("......etcpasswd");
    }

    @ParameterizedTest
    @CsvSource({
            "document_123.pdf, document_123.pdf",
            "éàùç, eauc",
            "my file.pdf, my_file.pdf"
    })
    void sanitizeFilename_should_transform_input_correctly(String input, String expected) {
        assertThat(FileUtility.sanitizeFilename(input)).isEqualTo(expected);
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
