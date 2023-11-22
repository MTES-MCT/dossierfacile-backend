package fr.dossierfacile.process.file.barcode;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.barcode.qrcode.QrCode;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

import static fr.dossierfacile.process.file.util.FileTypeUtil.isPdf;

public abstract class InMemoryFile implements AutoCloseable {

    private boolean hasQrCodeBeenSearched = false;
    private boolean has2DDocBeenSearched = false;

    private QrCode qrCode;
    private TwoDDocRawContent twoDDoc;

    private String contentAsString;

    public static InMemoryFile download(File file, FileStorageService fileStorageService) throws IOException {
        try (InputStream inputStream = fileStorageService.download(file.getStorageFile())) {
            if (isPdf(file)) {
                return new InMemoryPdfFile(PDDocument.load(inputStream));
            }
            return new InMemoryImageFile(ImageIO.read(inputStream));
        }
    }

    public String getContentAsString() {
        if (contentAsString == null) {
            contentAsString = readContentAsString();
        }
        return contentAsString;
    }

    protected abstract String readContentAsString();

    public boolean hasQrCode() {
        return getQrCode() != null;
    }

    public QrCode getQrCode() {
        if (!hasQrCodeBeenSearched) {
            qrCode = findQrCode();
            hasQrCodeBeenSearched = true;
        }
        return qrCode;
    }

    protected abstract QrCode findQrCode();

    public boolean has2DDoc() {
        return get2DDoc() != null;
    }

    public TwoDDocRawContent get2DDoc() {
        if (!has2DDocBeenSearched) {
            twoDDoc = find2DDoc();
            has2DDocBeenSearched = true;
        }
        return twoDDoc;
    }

    protected abstract TwoDDocRawContent find2DDoc();

    @Override
    public void close() throws Exception {

    }

}
