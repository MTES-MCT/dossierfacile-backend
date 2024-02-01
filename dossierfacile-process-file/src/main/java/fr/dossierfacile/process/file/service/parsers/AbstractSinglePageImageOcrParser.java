package fr.dossierfacile.process.file.service.parsers;

import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.utils.FileUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
public abstract class AbstractSinglePageImageOcrParser<T extends ParsedFile> implements FileParser<T> {

    private BufferedImage getImage(File file) throws IOException {
        BufferedImage image;
        if ("pdf".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {
            BufferedImage[] images = FileUtility.convertPdfToImage(file);
            if (images == null || images.length < 1) {
                throw new IllegalStateException("pdf file cannot be convert to images");
            }
            image = images[0];
        } else {
            image = ImageIO.read(file);
        }

        if (image == null) {
            throw new IllegalStateException("image cannot be extracted from file " + file.getName());
        }
        return image;
    }

    @Override
    public T parse(File file) {
        try {
            return parse(getImage(file));
        } catch (IOException e) {
            log.error("Unable to read Image");
            return null;
        }
    }

    abstract T parse(BufferedImage image);
}
