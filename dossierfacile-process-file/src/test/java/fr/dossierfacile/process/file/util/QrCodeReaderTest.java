package fr.dossierfacile.process.file.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeReaderTest {

    private InMemoryPdfFile file;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "unemployment-document.pdf, bb8faee8-7e9a-4cae-8d53-aa700a852e94",
            "scholarship-document.pdf, f0ee0a2d-0be6-48c2-bbec-f91d8925f998",
            "student-document.pdf, b76abaf7-152d-4d75-bb08-76a82a8f9db2"
    })
    void should_read_qr_code_on_pdf_file(String fileName, String expectedDocumentId) throws IOException {
        file = getFileFromClasspath("monfranceconnect/" + fileName);
        Optional<QrCode> optionalQrCode = QrCodeReader.findQrCodeOn(file);

        assertThat(optionalQrCode).isPresent()
                .hasValueSatisfying(qrCode -> assertThat(qrCode.getContent()).contains(expectedDocumentId));
    }

    @Test
    void should_return_empty_if_no_qr_code_found() throws IOException {
        file = getFileFromClasspath("test-document.pdf");
        Optional<QrCode> result = QrCodeReader.findQrCodeOn(file);
        assertThat(result).isEmpty();
    }

    @AfterEach
    void tearDown() throws IOException {
        file.close();
    }

    private InMemoryPdfFile getFileFromClasspath(String fileName) throws IOException {
        String path = "documents/" + fileName;
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            return new InMemoryPdfFile(PDDocument.load(inputStream));
        }
    }

}