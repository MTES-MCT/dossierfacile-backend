package fr.dossierfacile.process.file.barcode.twoddoc.reader;

record SquarePosition(Coordinates coordinates, int width) {

    SquarePosition scale(int factor) {
        return new SquarePosition(coordinates.scale(factor), width * factor);
    }

}
