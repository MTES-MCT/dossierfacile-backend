package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

@Slf4j
public class TwoDDocImageFinder extends TwoDDocFinder {

    private final BufferedImage image;

    public TwoDDocImageFinder(BufferedImage image) {
        super(FileCropper.fromImageSource(image));
        this.image = image;
    }

    @Override
    public Optional<TwoDDocRawContent> find2DDoc() {
        TwoDDocSize twoDDocSize = TwoDDocSize.on(image);
        List<SquarePosition> list = KnownTwoDDocLocations.stream()
                .map(location -> location.toCoordinates(image))
                .map(coordinates -> coordinates.toSquare(twoDDocSize.width()))
                .toList();
        return tryToFindTwoDDocIn(list);
    }

}
