package fr.dossierfacile.api.front.util;

import fr.dossierfacile.common.model.ValidatedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilePageCounterTest {

    @ParameterizedTest
    @CsvSource({
            "one-page.pdf, 1",
            "two-pages.pdf, 2",
    })
    void should_count_pdf_pages(String fileName, int expectedPageCount) throws IOException {
        var file = multipartFile(MediaType.APPLICATION_PDF, getFileBytes(fileName));

        int pageCount = pageCounter(file, "application/pdf").getTotalNumberOfPages();

        assertThat(pageCount).isEqualTo(expectedPageCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "image/png", "image/jpeg"})
    void should_count_other_files_as_one_page(String mimeType) throws IOException {
        var file = multipartFile(MediaType.APPLICATION_OCTET_STREAM, "Test".getBytes());

        int pageCount = pageCounter(file, mimeType).getTotalNumberOfPages();

        assertThat(pageCount).isEqualTo(1);
    }

    @Test
    void should_ignore_empty_files() throws IOException {
        var file = multipartFile(MediaType.APPLICATION_PDF, new byte[]{});

        int pageCount = pageCounter(file, "application/pdf").getTotalNumberOfPages();

        assertThat(pageCount).isEqualTo(0);
    }

    @Test
    void should_count_total_number_of_pages() throws IOException {
        List<ValidatedFile> validatedFiles = List.of(
                validated(multipartFile(MediaType.TEXT_PLAIN, "Test".getBytes()), "text/plain"),
                validated(multipartFile(MediaType.IMAGE_JPEG, "Test".getBytes()), "image/jpeg"),
                validated(multipartFile(MediaType.APPLICATION_PDF, getFileBytes("one-page.pdf")), "application/pdf"),
                validated(multipartFile(MediaType.APPLICATION_PDF, getFileBytes("two-pages.pdf")), "application/pdf"),
                validated(multipartFile(MediaType.APPLICATION_PDF, new byte[]{}), "application/pdf")
        );

        int pageCount = new FilePageCounter(validatedFiles).getTotalNumberOfPages();

        assertThat(pageCount).isEqualTo(5);
    }

    private FilePageCounter pageCounter(MultipartFile file, String mimeType) {
        return new FilePageCounter(List.of(validated(file, mimeType)));
    }

    private ValidatedFile validated(MultipartFile file, String mimeType) {
        return new ValidatedFile(file, mimeType);
    }

    private MockMultipartFile multipartFile(MediaType mediaType, byte[] bytes) {
        return new MockMultipartFile("name", "fileName", mediaType.toString(), bytes);
    }

    private byte[] getFileBytes(String fileName) throws IOException {
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            return resourceAsStream.readAllBytes();
        }
    }

}