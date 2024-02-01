package fr.dossierfacile.process.file.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.DigestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
}