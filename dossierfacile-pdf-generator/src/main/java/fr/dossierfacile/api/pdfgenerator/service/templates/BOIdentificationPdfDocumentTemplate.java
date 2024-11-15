package fr.dossierfacile.api.pdfgenerator.service.templates;

import fr.dossierfacile.api.pdfgenerator.configuration.FeatureFlipping;
import fr.dossierfacile.api.pdfgenerator.model.FileInputStream;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfSignatureService;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.PdfTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.List;

@Service
@Slf4j
@Qualifier("boIdentificationPdfDocumentTemplate")
public class BOIdentificationPdfDocumentTemplate extends BOPdfDocumentTemplate implements PdfTemplate<List<FileInputStream>> {

    public BOIdentificationPdfDocumentTemplate(MessageSource messageSource, FeatureFlipping featureFlipping, PdfSignatureService pdfSignatureService) {
        super(messageSource, featureFlipping, pdfSignatureService);
    }

    /**
     * create grid sample containing a small matrix computed from a source image
     */
    private int[][] createGridSample(float scale, BufferedImage sourceImage) {
        int gridWidth = (int) (sourceImage.getWidth() * scale);
        int gridHeight = (int) (sourceImage.getHeight() * scale);

        // convert image to small resolution image
        BufferedImage image = new BufferedImage(gridWidth, gridHeight, BufferedImage.TYPE_INT_RGB);

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.scale(scale, scale);

        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.drawImage(sourceImage, affineTransform, null);
        g.dispose();

        // compute grid
        int[][] grid = new int[gridWidth][gridHeight];

        Raster raster = image.getRaster();
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++)
                grid[x][y] = raster.getSample(x, y, 0);
        }
        return grid;
    }

    /**
     * returns true if specified column if blank
     */
    private boolean isBlankColumn(int[][] grid, int col) {
        for (int row = 0; row < grid[col].length; row++) {
            if (grid[col][row] < 250) {
                return false;
            }
        }
        return true;
    }

    /**
     * returns true if specified row if blank
     */
    private boolean isBlankLine(int[][] grid, int row) {
        for (int col = 0; col < grid.length; col++) {
            if (grid[col][row] < 250) {
                return false;
            }
        }
        return true;
    }

    /**
     * crop blank border on both axis
     */
    @Override
    protected BufferedImage smartCrop(BufferedImage image) {

        // then create a grid
        float scale = 48 / (float) image.getWidth();
        int[][] gridSample = createGridSample(scale, image);

        // crop X
        int col = 0;
        while (col < gridSample.length && isBlankColumn(gridSample, col)) {
            col++;
        }
        if (col == gridSample.length) {
            // unable to treat sounds empty
            log.warn("Image sounds empty");
            return image;
        }

        int maxCol = gridSample.length - 1;
        while (maxCol > col && isBlankColumn(gridSample, maxCol)) {
            maxCol--;
        }
        // crop Y
        int row = 0;
        while (row < gridSample[0].length && isBlankLine(gridSample, row)) {
            row++;
        }
        int maxRow = gridSample[0].length - 1;
        while (maxRow > row && isBlankLine(gridSample, maxRow)) {
            maxRow--;
        }
        maxRow = Math.min(maxRow + 2, gridSample[0].length);
        maxCol = Math.min(maxCol + 2, gridSample.length);

        int x = (int) (col / scale);
        int y = (int) (row / scale);
        int width = (int) (image.getWidth() - x - ((gridSample.length - maxCol) / scale));
        int height = (int) (image.getHeight() - y - ((gridSample[0].length - maxRow) / scale));

        return image.getSubimage(x, y, width, height);
    }

}

