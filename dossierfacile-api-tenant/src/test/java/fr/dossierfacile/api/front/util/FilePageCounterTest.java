package fr.dossierfacile.api.front.util;

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

        int pageCount = pageCounter(file).getTotalNumberOfPages();

        assertThat(pageCount).isEqualTo(expectedPageCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEXT_PLAIN", "IMAGE_PNG", "IMAGE_JPG"})
    void should_count_other_files_as_one_page(MediaType mediaType) throws IOException {
        var file = multipartFile(mediaType, "Test".getBytes());

        int pageCount = pageCounter(file).getTotalNumberOfPages();

        assertThat(pageCount).isEqualTo(1);
    }

    @Test
    void should_ignore_empty_files() throws IOException {
        var file = multipartFile(MediaType.APPLICATION_PDF, new byte[]{});

        int pageCount = pageCounter(file).getTotalNumberOfPages();

        assertThat(pageCount).isEqualTo(0);
    }

    @Test
    void should_count_total_number_of_pages() throws IOException {
        List<MultipartFile> files = List.of(
                multipartFile(MediaType.TEXT_PLAIN, "Test".getBytes()),
                multipartFile(MediaType.IMAGE_JPEG, "Test".getBytes()),
                multipartFile(MediaType.APPLICATION_PDF, getFileBytes("one-page.pdf")),
                multipartFile(MediaType.APPLICATION_PDF, getFileBytes("two-pages.pdf")),
                multipartFile(MediaType.APPLICATION_PDF, new byte[]{})
        );

        int pageCount = new FilePageCounter(files).getTotalNumberOfPages();

        assertThat(pageCount).isEqualTo(5);
    }

    private FilePageCounter pageCounter(MultipartFile file) {
        return new FilePageCounter(List.of(file));
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