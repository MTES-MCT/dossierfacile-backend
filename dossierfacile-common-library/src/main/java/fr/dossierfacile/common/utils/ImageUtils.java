package fr.dossierfacile.common.utils;

import java.awt.*;
import java.awt.image.*;

public final class ImageUtils {

    private ImageUtils(){}

    public static class GrayBytes {
        public final byte[] data;
        public final int width;
        public final int height;
        public final int stride;

        public GrayBytes(byte[] data, int width, int height, int stride) {
            this.data = data; this.width = width; this.height = height; this.stride = stride;
        }
    }

    public static GrayBytes toGrayBytes(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        // fond BLANC pour neutraliser l'alpha (sinon fond noir headless Linux)
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, gray.getWidth(), gray.getHeight());
        // pour des codes 1-bit, évite le flou
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, null);
        g.dispose();

        WritableRaster raster = gray.getRaster();
        DataBuffer db = raster.getDataBuffer();
        if (!(db instanceof DataBufferByte))
            throw new IllegalStateException("Unexpected buffer type: " + db.getClass());

        byte[] bytes = ((DataBufferByte) db).getData();
        int stride = ((ComponentSampleModel) raster.getSampleModel()).getScanlineStride();
        return new GrayBytes(bytes, gray.getWidth(), gray.getHeight(), stride);
    }
}
