package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import org.apache.pdfbox.text.TextPosition;

import java.util.Collection;
import java.util.List;

public class PdfTwoDDocLocator {

    private static final String TWO_D_DOC_TEXT_INDICATOR = "2D-DOC";

    private final PdfPage pdfPage;
    private final PdfTextExtractor pdfTextExtractor;
    private final TwoDDocSize twoDDocSize;

    PdfTwoDDocLocator(PdfPage pdfPage) {
        this.pdfPage = pdfPage;
        this.pdfTextExtractor = new PdfTextExtractor(pdfPage);
        this.twoDDocSize = TwoDDocSize.on(pdfPage);
    }

    List<SquarePosition> getPotentialPositionsBasedOnText() {
        return pdfTextExtractor
                .findText(TWO_D_DOC_TEXT_INDICATOR)
                .stream()
                .map(textPosition -> getTwoDDocUpperLeftCoordinates(textPosition, twoDDocSize))
                .flatMap(Collection::stream)
                .map(coordinates -> coordinates.toSquare(twoDDocSize.width()))
                .toList();
    }

    List<SquarePosition> getCommonlyKnownPositions() {
        if (pdfTextExtractor.documentContainsText() // we assume there's no 2D-Doc in this case because it should have been found in the text
                && !pdfTextExtractor.getDocumentText().contains(TWO_D_DOC_TEXT_INDICATOR) // unless the word 2D-Doc has been spotted elsewhere, in which case we should keep searching
        ) {
            return List.of();
        }
        // in case of an image, we explore known possibilities
        return KnownTwoDDocLocations.stream()
                .map(location -> location.toCoordinates(pdfPage))
                .map(coordinates -> coordinates.toSquare(twoDDocSize.width()))
                .toList();
    }

    private List<Coordinates> getTwoDDocUpperLeftCoordinates(TextPosition twoDDocTextPosition, TwoDDocSize twoDDocSize) {
        int squareWidth = twoDDocSize.width();
        Coordinates coordinates = Coordinates.of(twoDDocTextPosition);
        return switch ((int) twoDDocTextPosition.getDir()) {
            default -> List.of(
                    coordinates
                            .moveHorizontally(-squareWidth / 2)
                            .moveVertically(-squareWidth),
                    coordinates
                            .moveHorizontally(-squareWidth / 2)
            );
            case 90 -> List.of(
                    coordinates
                            .moveVertically(-squareWidth / 2)
            );
            case 270 -> List.of(
                    coordinates
                            .moveHorizontally(-squareWidth)
                            .moveVertically(-squareWidth / 2)
            );
        };
    }

}
