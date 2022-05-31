package fr.dossierfacile.api.pdfgenerator.model;

/**
 * Useful class to store page dimension
 */
public class PageDimension {
    public static PageDimension A4128 = new PageDimension(1058, 1496, 128);
    public static PageDimension A4150 = new PageDimension(1240, 1754, 150);
    public final int width;
    public final int height;
    public final int dpi;

    public PageDimension(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }
}