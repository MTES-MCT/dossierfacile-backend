package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class TwoDDocPdfFinder extends TwoDDocFinder {

    private final PdfPage pdfPage;

    private TwoDDocPdfFinder(PdfPage pdfPage, FileCropper fileCropper) {
        super(fileCropper);
        this.pdfPage = pdfPage;
    }

    public static TwoDDocPdfFinder on(PDDocument document) throws IOException {
        return on(document, 0);
    }

    public static TwoDDocPdfFinder on(PDDocument document, int pageIndex) throws IOException {
        PdfPage pdfPage = new PdfPage(document, pageIndex);
        return new TwoDDocPdfFinder(pdfPage, FileCropper.fromPdfSource(pdfPage));
    }

    @Override
    public Optional<TwoDDocRawContent> find2DDoc() {
        if (pdfPage.document().isEncrypted()) {
            return Optional.empty();
        }

        PdfTwoDDocLocator twoDDocLocator = new PdfTwoDDocLocator(pdfPage);

        Optional<TwoDDocRawContent> twoDDoc = tryToFindTwoDDocIn(twoDDocLocator.getPotentialPositionsBasedOnText());
        if (twoDDoc.isPresent()) {
            return twoDDoc;
        }

        return tryToFindTwoDDocIn(twoDDocLocator.getCommonlyKnownPositions());
    }

}
