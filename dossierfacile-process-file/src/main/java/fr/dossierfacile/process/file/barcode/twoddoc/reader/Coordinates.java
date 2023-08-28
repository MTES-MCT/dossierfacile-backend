package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import org.apache.pdfbox.text.TextPosition;

/**
 * Representation of a point in a cartesian referential.
 * <p>
 * Note: For PDF manipulation, the origin is located in the upper-left corner
 * of the page.
 *
 * @param x – the X coordinate of the point
 * @param y – the Y coordinate of the point
 */
record Coordinates(int x, int y) {

    Coordinates(float x, float y) {
        this((int) x, (int) y);
    }

    static Coordinates of(TextPosition position) {
        return new Coordinates(position.getX(), position.getY());
    }

    Coordinates scale(int factor) {
        return new Coordinates(x * factor, y * factor);
    }

    Coordinates moveHorizontally(int delta) {
        return new Coordinates(x + delta, y);
    }

    Coordinates moveVertically(int delta) {
        return new Coordinates(x, y + delta);
    }

    SquarePosition toSquare(int width) {
        return new SquarePosition(this, width);
    }

}
