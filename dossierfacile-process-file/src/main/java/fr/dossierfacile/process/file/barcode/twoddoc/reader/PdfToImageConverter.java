package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import lombok.AllArgsConstructor;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;

@AllArgsConstructor
public class PdfToImageConverter {

    private final BufferedImage image;
    private final int scale;

    static PdfToImageConverter of(PdfPage pdfPage) throws IOException {
        int scale = Math.max(1, 2048 / pdfPage.getWidth());

        PDFRenderer pdfRenderer = new PDFRenderer(pdfPage.document());
        BufferedImage image = pdfRenderer.renderImage(pdfPage.pageIndex(), scale, ImageType.BINARY);
        return new PdfToImageConverter(image, scale);
    }

    BufferedImage getSubImageAt(SquarePosition position) {
        SquarePosition scaledPosition = position.scale(scale);
        Coordinates coordinates = scaledPosition.coordinates();
        int width = scaledPosition.width();

        int x = Math.min(Math.max(0, coordinates.x()), image.getWidth() - width);
        int y = Math.min(Math.max(0, coordinates.y()), image.getHeight() - width);

        return image.getSubimage(x, y, width, width);
    }

}
