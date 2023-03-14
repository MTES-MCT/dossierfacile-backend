package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class InMemoryPdfFile implements AutoCloseable {

    private final PDDocument pdfBoxDocument;

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

    public Optional<QrCode> findQrCode() {
        return QrCodeReader.findQrCodeOn(this);
    }

    public PDDocument getPdfBoxDocument() {
        return pdfBoxDocument;
    }

    @Override
    public void close() throws IOException {
        pdfBoxDocument.close();
    }

}
