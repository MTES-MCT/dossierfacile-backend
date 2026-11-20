package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

/**
 * Describes one page of a piece, ready for the layout of the dossier PDF.
 *
 * @param file    the JPEG image of the page, or the PDF that contains it; the pages of a PDF share the same array
 * @param pdfPage the number of the page in the PDF, starting from 1, or 0 for an image
 * @param width   the width of the displayed page; the unit is free, because only the aspect ratio matters
 * @param height  the height of the displayed page, in the same unit
 * @param text    the text of the page, which is empty for a scan
 */
public record PieceContent(byte[] file, int pdfPage, float width, float height, String text) {

    static PieceContent image(byte[] jpeg, int width, int height) {
        return new PieceContent(jpeg, 0, width, height, "");
    }

    boolean isImage() {
        return pdfPage == 0;
    }
}
