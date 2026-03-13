package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.common.validator.AllowedMimeTypesValidator;
import fr.dossierfacile.common.validator.annotation.AllowedMimeTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non-regression test: verifies that @AllowedMimeTypes (replacing former @Extension)
 * accepts the same file types: application/pdf, image/jpeg, image/png, image/heif.
 */
class AllowedMimeTypesIntegrationTest {

    private AllowedMimeTypesValidator validator;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        validator = new AllowedMimeTypesValidator();
        AllowedMimeTypes annotation = DocumentForm.class.getDeclaredField("documents").getAnnotation(AllowedMimeTypes.class);
        validator.initialize(annotation);
    }

    @Test
    void should_accept_pdf() {
        MultipartFile file = new MockMultipartFile("doc", "doc.pdf", "application/pdf", "%PDF-1.4".getBytes());
        assertThat(validator.isValid(List.of(file), null)).isTrue();
    }

    @Test
    void should_accept_jpeg() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MultipartFile file = new MockMultipartFile("doc", "img.jpg", "image/jpeg", jpegBytes);
        assertThat(validator.isValid(List.of(file), null)).isTrue();
    }

    @Test
    void should_accept_png() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        MultipartFile file = new MockMultipartFile("doc", "img.png", "image/png", pngBytes);
        assertThat(validator.isValid(List.of(file), null)).isTrue();
    }

    @Test
    void should_reject_unsupported_type() {
        MultipartFile file = new MockMultipartFile("doc", "file.exe", "application/x-msdownload", "MZ".getBytes());
        assertThat(validator.isValid(List.of(file), null)).isFalse();
    }
}
