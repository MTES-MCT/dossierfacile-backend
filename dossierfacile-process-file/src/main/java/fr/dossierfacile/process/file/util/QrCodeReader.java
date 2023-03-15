package fr.dossierfacile.process.file.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

@Slf4j
public class QrCodeReader {

    public static Optional<QrCode> findQrCodeOn(PDDocument document) {
        if (document.isEncrypted()) {
            return Optional.empty();
        }
        try {
            BinaryBitmap binaryBitmap = buildBinaryBitmap(document);
            String qrCodeContent = new QRCodeReader().decode(binaryBitmap).getText();
            log.info("Found QR code on document (content: {})", qrCodeContent);

            return Optional.ofNullable(qrCodeContent).map(QrCode::new);
        } catch (NotFoundException e) {
            log.info("No QR code found on document");
        } catch (IOException | ChecksumException | FormatException e) {
            log.error("Exception while trying to extract QR code from pdf (Sentry ID: {})", Sentry.captureException(e), e);
        }
        return Optional.empty();
    }

    private static BinaryBitmap buildBinaryBitmap(PDDocument document) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        float dpi = (1058 / document.getPage(0).getMediaBox().getWidth()) * 300;
        dpi = Math.min(600, dpi);
        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, dpi, ImageType.ARGB);

        return new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(bufferedImage)));
    }

}