package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.process.file.barcode.twoddoc.reader.TwoDDocImageReader.readTwoDDocOn;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TwoDDocFinder {

    private final PdfPage pdfPage;
    private final PdfToImageConverter pdfToImageConverter;

    public static TwoDDocFinder on(PDDocument document) throws IOException {
        return on(document, 0);
    }

    public static TwoDDocFinder on(PDDocument document, int pageIndex) throws IOException {
        PdfPage pdfPage = new PdfPage(document, pageIndex);
        return new TwoDDocFinder(pdfPage, PdfToImageConverter.of(pdfPage));
    }

    public Optional<TwoDDocRawContent> find2DDoc() {
        if (pdfPage.document().isEncrypted()) {
            return Optional.empty();
        }

        PdfTwoDDocLocator twoDDocLocator = new PdfTwoDDocLocator(pdfPage);

        Optional<TwoDDocRawContent> twoDDoc = explore(twoDDocLocator.getPotentialPositionsBasedOnText());
        if (twoDDoc.isPresent()) {
            return twoDDoc;
        }

        return explore(twoDDocLocator.getCommonlyKnownPositions());
    }

    private Optional<TwoDDocRawContent> explore(List<SquarePosition> squarePositions) {
        for (SquarePosition position : squarePositions) {
            BufferedImage imageToRead = pdfToImageConverter.getSubImageAt(position);
            Optional<TwoDDocRawContent> twoDDoc = readTwoDDocOn(imageToRead);
            if (twoDDoc.isPresent()) {
                return twoDDoc;
            }
        }
        return Optional.empty();
    }

}
