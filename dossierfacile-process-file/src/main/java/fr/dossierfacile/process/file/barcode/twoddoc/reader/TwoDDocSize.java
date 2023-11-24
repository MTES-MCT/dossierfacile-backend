package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import java.awt.image.BufferedImage;

record TwoDDocSize(int width) {

    private static final float TWO_D_DOC_WIDTH_RELATIVE_TO_FILE_WIDTH = 0.2f;

    static TwoDDocSize on(PdfPage pdfPage) {
        return new TwoDDocSize((int) (TWO_D_DOC_WIDTH_RELATIVE_TO_FILE_WIDTH * pdfPage.getWidth()));
    }

    static TwoDDocSize on(BufferedImage image) {
        return new TwoDDocSize((int) (TWO_D_DOC_WIDTH_RELATIVE_TO_FILE_WIDTH * image.getWidth()));
    }

}
