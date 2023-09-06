package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import org.apache.pdfbox.pdmodel.PDDocument;

public record PdfPage(PDDocument document, int pageIndex) {

    public int getWidth() {
        return (int) document.getPage(pageIndex).getMediaBox().getWidth();
    }

    public int getHeight() {
        return (int) document.getPage(pageIndex).getMediaBox().getHeight();
    }

}
