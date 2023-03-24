package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
public class InMemoryPdfFile implements AutoCloseable {

    private final PDDocument pdfBoxDocument;

    private boolean hasQrCodeBeenSearched = false;
    private QrCode qrCode;
    private String contentAsString;

    public static InMemoryPdfFile create(File file, FileStorageService fileStorageService) throws IOException {
        try (InputStream inputStream = fileStorageService.download(file)) {
            return new InMemoryPdfFile(PDDocument.load(inputStream));
        }
    }

    public String getContentAsString() {
        if (contentAsString == null) {
            contentAsString = readContentAsString();
        }
        return contentAsString;
    }

    private String readContentAsString() {
        if (pdfBoxDocument.isEncrypted()) {
            return "";
        }
        try {
            PDFTextStripper reader = new PDFTextStripper();
            reader.setAddMoreFormatting(true);
            return reader.getText(pdfBoxDocument);
        } catch (IOException e) {
            Sentry.captureException(e);
            return "";
        }
    }

    public boolean hasQrCode() {
        return getQrCode() != null;
    }

    public QrCode getQrCode() {
        if (!hasQrCodeBeenSearched) {
            qrCode = QrCodeReader.findQrCodeOn(pdfBoxDocument).orElse(null);
            hasQrCodeBeenSearched = true;
        }
        return qrCode;
    }

    @Override
    public void close() throws IOException {
        pdfBoxDocument.close();
    }

}
