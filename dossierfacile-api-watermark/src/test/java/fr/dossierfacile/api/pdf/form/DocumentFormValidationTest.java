package fr.dossierfacile.api.pdf.form;

import fr.dossierfacile.common.validator.AllowedMimeTypesValidator;
import fr.dossierfacile.common.validator.SizeFileValidator;
import fr.dossierfacile.common.validator.annotation.AllowedMimeTypes;
import fr.dossierfacile.common.validator.annotation.SizeFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFormValidationTest {

    private AllowedMimeTypesValidator allowedMimeValidator;
    private SizeFileValidator sizeFileValidator;

    @BeforeEach
    void setUp() {
        allowedMimeValidator = new AllowedMimeTypesValidator();
        allowedMimeValidator.initialize(TestForm.class.getDeclaredFields()[0].getAnnotation(AllowedMimeTypes.class));
        sizeFileValidator = new SizeFileValidator();
        sizeFileValidator.initialize(TestForm.class.getDeclaredFields()[0].getAnnotation(SizeFile.class));
    }

    // --- Accepted MIME types ---

    static Stream<Arguments> acceptedFiles() {
        return Stream.of(
                Arguments.of("doc.pdf",   "%PDF-1.4".getBytes()),
                Arguments.of("photo.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})
        );
    }

    @ParameterizedTest(name = "{0} should be accepted")
    @MethodSource("acceptedFiles")
    void should_accept_file_with_allowed_mime_type(String filename, byte[] content) {
        MultipartFile file = new MockMultipartFile("file", filename, null, content);
        assertThat(allowedMimeValidator.isValid(List.of(file), null)).isTrue();
    }

    // --- Rejected MIME types ---

    static Stream<Arguments> rejectedFiles() {
        return Stream.of(
                Arguments.of("script.exe",  "MZ".getBytes(),                                            "exe declared as-is"),
                Arguments.of("malware.jpg", "MZ executable content".getBytes(),                         "exe disguised as jpeg"),
                Arguments.of("doc.pdf",     "<!DOCTYPE html><html><body>evil</body></html>".getBytes(), "html disguised as pdf")
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("rejectedFiles")
    void should_reject_file_with_disallowed_or_forged_mime_type(String filename, byte[] content, String description) {
        MultipartFile file = new MockMultipartFile("file", filename, null, content);
        assertThat(allowedMimeValidator.isValid(List.of(file), null)).isFalse();
    }

    // --- Size validation ---

    @Test
    void should_reject_file_over_20mb() {
        MultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[21 * 1024 * 1024]);
        assertThat(sizeFileValidator.isValid(List.of(file), null)).isFalse();
    }

    // --- Edge cases ---

    @Test
    void should_accept_empty_file_list() {
        assertThat(allowedMimeValidator.isValid(List.of(), null)).isTrue();
        assertThat(sizeFileValidator.isValid(List.of(), null)).isTrue();
    }

    static class TestForm {
        @AllowedMimeTypes({"application/pdf", "image/jpeg", "image/png", "image/heif"})
        @SizeFile(max = 20)
        List<MultipartFile> files;
    }
}
