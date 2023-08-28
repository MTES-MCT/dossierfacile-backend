package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.stream.Stream;

@AllArgsConstructor
enum KnownTwoDDocLocations {

    TAX_STATEMENT(0.2f, 0.05f),
    PUBLIC_SERVICE_PAYSLIP(0.75f, 0.8f),
    SNCF_PAYSLIP(0.7f, 0.03f),
    FREE_INVOICE(0f, 0.15f),
    ;

    private final float xFactor;
    private final float yFactor;

    static Stream<KnownTwoDDocLocations> stream() {
        return Arrays.stream(KnownTwoDDocLocations.values());
    }

    Coordinates toCoordinates(PdfPage pdfPage) {
        return new Coordinates(xFactor * pdfPage.getWidth(), yFactor * pdfPage.getHeight());
    }

}
