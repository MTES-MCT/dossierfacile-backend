package fr.dossierfacile.api.pdf.form;

import fr.dossierfacile.common.validator.SizeFileValidator;
import fr.dossierfacile.common.validator.annotation.SizeFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFormValidationTest {

    private SizeFileValidator sizeFileValidator;

    @BeforeEach
    void setUp() {
        sizeFileValidator = new SizeFileValidator();
        sizeFileValidator.initialize(TestForm.class.getDeclaredFields()[0].getAnnotation(SizeFile.class));
    }

    @Test
    void should_reject_file_over_20mb() {
        MultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[21 * 1024 * 1024]);
        assertThat(sizeFileValidator.isValid(List.of(file), null)).isFalse();
    }

    @Test
    void should_accept_empty_file_list() {
        assertThat(sizeFileValidator.isValid(List.of(), null)).isTrue();
    }

    static class TestForm {
        @SizeFile(max = 20)
        List<MultipartFile> files;
    }
}
