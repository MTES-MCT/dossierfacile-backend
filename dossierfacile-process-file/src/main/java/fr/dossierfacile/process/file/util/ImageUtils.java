package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.utils.FileUtility;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.DigestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@UtilityClass
public class ImageUtils {

    public static String md5(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            byte[] bytes = baos.toByteArray();
            return DigestUtils.md5DigestAsHex(bytes);
        }
    }

    public static BufferedImage[] getImagesFromFile(File file) throws IOException {
        if ("pdf".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) {
            BufferedImage[] images = FileUtility.convertPdfToImage(file);
            if (images == null || images.length < 1) {
                throw new IllegalStateException("pdf file cannot be convert to images");
            }
            return images;
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IllegalStateException("image cannot be extracted from file " + file.getName());
        }
        return new BufferedImage[]{image};
    }
}