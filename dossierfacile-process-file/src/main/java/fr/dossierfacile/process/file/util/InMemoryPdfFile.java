package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
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

    public static InMemoryPdfFile create(File file, FileStorageService fileStorageService) throws IOException {
        try (InputStream inputStream = fileStorageService.download(file)) {
            return new InMemoryPdfFile(PDDocument.load(inputStream));
        }
    }

    public String readContentAsString() {
        return readContentAsString(Integer.MAX_VALUE);
    }

    public String readContentAsString(int endPage) {
        if (pdfBoxDocument.isEncrypted()) {
            return "";
        }
        try {
            PDFTextStripper reader = new PDFTextStripper();
            reader.setAddMoreFormatting(true);
            reader.setEndPage(endPage);
            return reader.getText(pdfBoxDocument);
        } catch (IOException e) {
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
