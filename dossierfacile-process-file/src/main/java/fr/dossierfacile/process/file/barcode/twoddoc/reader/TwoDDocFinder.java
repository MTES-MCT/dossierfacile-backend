package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.process.file.barcode.twoddoc.reader.TwoDDocDecoder.readTwoDDocOn;

@Slf4j
@AllArgsConstructor
public abstract class TwoDDocFinder {

    final FileCropper fileCropper;

    public abstract Optional<TwoDDocRawContent> find2DDoc();

    Optional<TwoDDocRawContent> tryToFindTwoDDocIn(List<SquarePosition> squarePositions) {
        for (SquarePosition position : squarePositions) {
            CroppedFile croppedFile = fileCropper.cropAt(position);
            // Zxing seems to work better on a rotated 2D-Doc
            Optional<TwoDDocRawContent> twoDDoc = readTwoDDocOn(croppedFile.rotate(180));
            if (twoDDoc.isPresent()) {
                log.info("Found 2D-Doc (with 180° rotation)");
                return twoDDoc;
            }
            twoDDoc = readTwoDDocOn(croppedFile);
            if (twoDDoc.isPresent()) {
                log.info("Found 2D-Doc (with no rotation)");
                return twoDDoc;
            }
        }
        return Optional.empty();
    }

}
