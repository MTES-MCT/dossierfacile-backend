package fr.dossierfacile.process.file.barcode.twoddoc;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

@Slf4j
public class TwoDDocReader {

    private static final DataMatrixReader READER = new DataMatrixReader();

    public static Optional<TwoDDocRawContent> find2DDocOn(PDDocument document) {
        if (document.isEncrypted()) {
            return Optional.empty();
        }

        for (PossibleCodePosition possibleCodePosition : PossibleCodePosition.values()) {
            Optional<Result> result = tryReadingCode(document, possibleCodePosition);
            if (result.isPresent()) {
                return result.map(Result::getText).map(TwoDDocRawContent::new);
            }
        }

        return Optional.empty();
    }

    private static Optional<Result> tryReadingCode(PDDocument document, PossibleCodePosition possibleCodePosition) {
        try {
            BinaryBitmap binaryBitmap = possibleCodePosition.buildBinaryBitmap(document);
            return Optional.of(READER.decode(binaryBitmap));
        } catch (NotFoundException e) {
            return Optional.empty();
        } catch (FormatException | ChecksumException e) {
            log.info("Format or checksum exception on found 2D-Doc", e);
            return Optional.empty();
        } catch (IOException e) {
            log.error("IOException while trying to extract 2D-Doc from pdf", e);
            return Optional.empty();
        }
    }

    @AllArgsConstructor
    private enum PossibleCodePosition {

        TAX_STATEMENT(20, 5),
        PUBLIC_SERVICE_PAYSLIP(75, 80),
        SNCF_PAYSLIP(70, 3),
        FREE_INVOICE(0, 15)
        ;

        private final double xPercent;
        private final double yPercent;

        BinaryBitmap buildBinaryBitmap(PDDocument document) throws IOException {
            int scale = Math.max(1, (int) (2048 / document.getPage(0).getMediaBox().getWidth()));

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage bufferedImage = pdfRenderer.renderImage(0, scale, ImageType.BINARY);

            return new BinaryBitmap(
                    new HybridBinarizer(
                            new BufferedImageLuminanceSource(
                                    cropImage(bufferedImage)
                            )));
        }

        private BufferedImage cropImage(BufferedImage image) {
            int x = (int) xPercent * image.getWidth() / 100;
            int y = (int) yPercent * image.getHeight() / 100;
            int width = 25 * image.getWidth() / 100;
            return image.getSubimage(x, y, width, width);
        }

    }

}
