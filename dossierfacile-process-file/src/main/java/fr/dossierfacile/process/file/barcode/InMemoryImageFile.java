package fr.dossierfacile.process.file.barcode;

import fr.dossierfacile.process.file.barcode.qrcode.QrCode;
import fr.dossierfacile.process.file.barcode.qrcode.QrCodeReader;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import fr.dossierfacile.process.file.barcode.twoddoc.reader.TwoDDocImageFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;

@Slf4j
@RequiredArgsConstructor
public class InMemoryImageFile extends InMemoryFile {

    private final BufferedImage image;

    @Override
    protected String readContentAsString() {
        return ""; // TODO plug ocr ?
    }

    @Override
    public QrCode findQrCode() {
        return QrCodeReader.findQrCodeOn(image).orElse(null);
    }

    @Override
    protected TwoDDocRawContent find2DDoc() {
        return new TwoDDocImageFinder(image).find2DDoc().orElse(null);
    }

}
