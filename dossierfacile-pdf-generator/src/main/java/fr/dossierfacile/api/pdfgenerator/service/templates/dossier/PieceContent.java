package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import java.util.Arrays;
import java.util.Objects;

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
        return new PieceContent(jpeg, 0, (float) width, (float) height, "");
    }

    boolean isImage() {
        return pdfPage == 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PieceContent(byte[] otherFile, int otherPdfPage, float otherWidth, float otherHeight, String otherText)
                && Arrays.equals(file, otherFile)
                && pdfPage == otherPdfPage
                && Float.compare(width, otherWidth) == 0
                && Float.compare(height, otherHeight) == 0
                && Objects.equals(text, otherText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(file), pdfPage, width, height, text);
    }

    /** Leaves out the content of the file and the text of the page, which are personal data. */
    @Override
    public String toString() {
        return "PieceContent[file=" + file.length + " bytes, pdfPage=" + pdfPage + ", width=" + width + ", height=" + height + "]";
    }
}
