package fr.dossierfacile.process.file.barcode;

import fr.dossierfacile.process.file.barcode.qrcode.QrCode;
import fr.dossierfacile.process.file.barcode.qrcode.QrCodeReader;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import fr.dossierfacile.process.file.barcode.twoddoc.reader.TwoDDocPdfFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class InMemoryPdfFile extends InMemoryFile {

    private final PDDocument pdfBoxDocument;

    @Override
    protected String readContentAsString() {
        if (pdfBoxDocument.isEncrypted()) {
            return "";
        }
        try {
            PDFTextStripper reader = new PDFTextStripper();
            reader.setAddMoreFormatting(true);
            return reader.getText(pdfBoxDocument);
        } catch (IOException e) {
            log.error("Unable to read", e);
            return "";
        }
    }

    @Override
    public QrCode findQrCode() {
        return QrCodeReader.findQrCodeOn(pdfBoxDocument).orElse(null);
    }

    @Override
    public TwoDDocRawContent find2DDoc() {
        try {
            return TwoDDocPdfFinder.on(pdfBoxDocument).find2DDoc().orElse(null);
        } catch (IOException e) {
            log.warn("Failed to initialize 2D-Doc search", e);
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        pdfBoxDocument.close();
    }

}
