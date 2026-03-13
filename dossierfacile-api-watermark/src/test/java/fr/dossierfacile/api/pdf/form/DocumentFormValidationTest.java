package fr.dossierfacile.api.pdf.form;

import fr.dossierfacile.common.validator.AllowedMimeTypesValidator;
import fr.dossierfacile.common.validator.SizeFileValidator;
import fr.dossierfacile.common.validator.annotation.AllowedMimeTypes;
import fr.dossierfacile.common.validator.annotation.SizeFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @Test
    void should_accept_valid_pdf() {
        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-1.4".getBytes());
        assertThat(allowedMimeValidator.isValid(List.of(file), null)).isTrue();
        assertThat(sizeFileValidator.isValid(List.of(file), null)).isTrue();
    }

    @Test
    void should_reject_file_over_20mb() {
        byte[] content = new byte[21 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", content);
        assertThat(sizeFileValidator.isValid(List.of(file), null)).isFalse();
    }

    @Test
    void should_reject_invalid_file_type() {
        MultipartFile file = new MockMultipartFile("file", "script.exe", "application/x-msdownload", "MZ".getBytes());
        assertThat(allowedMimeValidator.isValid(List.of(file), null)).isFalse();
    }

    @Test
    void should_reject_forged_content_type_executable_claims_to_be_jpeg() {
        byte[] exeContent = "MZ executable content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "malware.jpg", "image/jpeg", exeContent);
        assertThat(allowedMimeValidator.isValid(List.of(file), null)).isFalse();
    }

    @Test
    void should_reject_forged_content_type_html_claims_to_be_pdf() {
        byte[] htmlContent = "<!DOCTYPE html><html><body>evil</body></html>".getBytes();
        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", htmlContent);
        assertThat(allowedMimeValidator.isValid(List.of(file), null)).isFalse();
    }

    @Test
    void should_accept_file_when_content_matches_declared_type() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);
        assertThat(allowedMimeValidator.isValid(List.of(file), null)).isTrue();
    }

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
