package fr.dossierfacile.process.file.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeReaderTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "unemployment-document.pdf, bb8faee8-7e9a-4cae-8d53-aa700a852e94",
            "scholarship-document.pdf, f0ee0a2d-0be6-48c2-bbec-f91d8925f998",
            "student-document.pdf, b76abaf7-152d-4d75-bb08-76a82a8f9db2"
    })
    void should_read_qr_code_on_pdf_file(String fileName, String expectedDocumentId) {
        InputStream pdfInputStream = getAsStream(fileName);
        Optional<String> qrCodeContent = QrCodeReader.extractQrContentFrom(pdfInputStream);

        assertThat(qrCodeContent).isPresent()
                .hasValueSatisfying(expectedDocumentId::contains);
    }

    @Test
    void should_return_empty_if_no_qr_code_found() {
        Optional<String> result = QrCodeReader.extractQrContentFrom(getAsStream("blank-document.pdf"));
        assertThat(result).isEmpty();
    }

    private InputStream getAsStream(String fileName) {
        return getClass().getClassLoader().getResourceAsStream("monfranceconnect/" + fileName);
    }

}