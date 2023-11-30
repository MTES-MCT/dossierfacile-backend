package fr.dossierfacile.process.file.barcode.qrcode;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import static fr.dossierfacile.process.file.TestFilesUtil.getImage;
import static fr.dossierfacile.process.file.TestFilesUtil.getPdfBoxDocument;
import static org.assertj.core.api.Assertions.assertThat;

class QrCodeReaderTest {

    private PDDocument document;

    @Test
    void should_read_qr_code_on_pdf_file() throws IOException {
        document = getPdfBoxDocument("qr-code.pdf");
        Optional<QrCode> optionalQrCode = QrCodeReader.findQrCodeOn(document);

        QrCode expectedCode = new QrCode("This is a QR code for testing");
        assertThat(optionalQrCode).isPresent().contains(expectedCode);
    }

    @Test
    void should_read_qr_code_on_image() throws IOException {
        BufferedImage image = getImage("qr-code.jpg");
        Optional<QrCode> optionalQrCode = QrCodeReader.findQrCodeOn(image);

        QrCode expectedCode = new QrCode("This is a QR code for testing");
        assertThat(optionalQrCode).isPresent().contains(expectedCode);
    }

    @Test
    void should_return_empty_if_no_qr_code_found() throws IOException {
        document = getPdfBoxDocument("test-document.pdf");
        Optional<QrCode> result = QrCodeReader.findQrCodeOn(document);
        assertThat(result).isEmpty();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (document != null) {
            document.close();
        }
    }

}