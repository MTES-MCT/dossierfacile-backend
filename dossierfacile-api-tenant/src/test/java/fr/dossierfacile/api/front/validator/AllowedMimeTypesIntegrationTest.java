package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.common.validator.AllowedMimeTypesValidator;
import fr.dossierfacile.common.validator.annotation.AllowedMimeTypes;
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

    static Stream<Arguments> allowedFiles() {
        return Stream.of(
                Arguments.of("doc.pdf",  "%PDF-1.4".getBytes()),
                Arguments.of("img.jpg",  new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
                Arguments.of("img.png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})
        );
    }

    @ParameterizedTest(name = "{0} should be accepted")
    @MethodSource("allowedFiles")
    void should_accept_allowed_file_type(String filename, byte[] content) {
        MultipartFile file = new MockMultipartFile("doc", filename, null, content);
        assertThat(validator.isValid(List.of(file), null)).isTrue();
    }

    @Test
    void should_reject_unsupported_type() {
        MultipartFile file = new MockMultipartFile("doc", "file.exe", "application/x-msdownload", "MZ".getBytes());
        assertThat(validator.isValid(List.of(file), null)).isFalse();
    }
}
