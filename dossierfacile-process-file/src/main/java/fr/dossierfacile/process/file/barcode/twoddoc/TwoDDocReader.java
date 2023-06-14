package fr.dossierfacile.process.file.barcode.twoddoc;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class TwoDDocReader {

	private static final Map<DecodeHintType, Object> HINTS;

    static {
        HINTS = new EnumMap<>(DecodeHintType.class);
        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
    }

    public static Optional<TwoDDocRawContent> find2DDocOn(PDDocument document) {
        if (document.isEncrypted()) {
            return Optional.empty();
        }

        try {
            BinaryBitmap binaryBitmap = buildBinaryBitmap(document);
            MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(new MultiFormatReader());
            Result[] results = multiReader.decodeMultiple(binaryBitmap, HINTS);
            return Optional.ofNullable(results[0].getText())
                    .map(TwoDDocRawContent::new);
        } catch (NotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            log.error("Exception while trying to extract 2D-Doc from pdf (Sentry ID: {})", Sentry.captureException(e), e);
            return Optional.empty();
        }
    }

    private static BinaryBitmap buildBinaryBitmap(PDDocument document) throws IOException {
        int scale = Math.max(1, (int) (2048 / document.getPage(0).getMediaBox().getWidth()));

        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage bufferedImage = pdfRenderer.renderImage(0, scale, ImageType.BINARY);

        // Crop image to have better recognition by the BarcodeReader library
        int x = 180 * bufferedImage.getWidth() / 1000;
        int y = 105 * bufferedImage.getWidth() / 1000;
        int width = 220 * bufferedImage.getWidth() / 1000;
        BufferedImage cropImg = bufferedImage.getSubimage(x, y, width, width);

        return new BinaryBitmap(
                new HybridBinarizer(
                        new BufferedImageLuminanceSource(cropImg)
                ));
    }

}
