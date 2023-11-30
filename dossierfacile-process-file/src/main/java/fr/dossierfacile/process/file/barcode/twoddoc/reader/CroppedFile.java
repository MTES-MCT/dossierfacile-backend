package fr.dossierfacile.process.file.barcode.twoddoc.reader;

import java.awt.*;
import java.awt.image.BufferedImage;

record CroppedFile(BufferedImage bufferedImage) {

    CroppedFile rotate(int degrees) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        BufferedImage result = new BufferedImage(width, height, bufferedImage.getType());
        Graphics2D graphics2D = result.createGraphics();

        graphics2D.rotate(Math.toRadians(degrees), (double) width / 2, (double) height / 2);
        graphics2D.drawImage(bufferedImage, null, 0, 0);

        return new CroppedFile(result);
    }

}
