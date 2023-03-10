package fr.dossierfacile.process.file.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Optional;

import static fr.dossierfacile.process.file.TestFilesUtil.getPdfFile;
import static org.assertj.core.api.Assertions.assertThat;

class QrCodeReaderTest {

    private InMemoryPdfFile file;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "unemployment-document.pdf, bb8faee8-7e9a-4cae-8d53-aa700a852e94",
            "scholarship-document.pdf, f0ee0a2d-0be6-48c2-bbec-f91d8925f998",
            "student-document.pdf, b76abaf7-152d-4d75-bb08-76a82a8f9db2",
            "tax-document.pdf, 5adf42fd-71b8-4515-ba16-37316fef19e7"
    })
    void should_read_qr_code_on_pdf_file(String fileName, String expectedDocumentId) throws IOException {
        file = getPdfFile("monfranceconnect/" + fileName);
        Optional<QrCode> optionalQrCode = QrCodeReader.findQrCodeOn(file);

        assertThat(optionalQrCode).isPresent()
                .hasValueSatisfying(qrCode -> assertThat(qrCode.getContent()).contains(expectedDocumentId));
    }

    @Test
    void should_return_empty_if_no_qr_code_found() throws IOException {
        file = getPdfFile("test-document.pdf");
        Optional<QrCode> result = QrCodeReader.findQrCodeOn(file);
        assertThat(result).isEmpty();
    }

    @AfterEach
    void tearDown() throws IOException {
        file.close();
    }

}