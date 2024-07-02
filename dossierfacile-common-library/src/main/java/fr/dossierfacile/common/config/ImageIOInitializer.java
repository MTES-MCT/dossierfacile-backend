package fr.dossierfacile.common.config;

import javax.imageio.ImageIO;

/**
 * Allows to load images codecs on threads
 */
public class ImageIOInitializer {
    public static void initialize() {
        ImageIO.scanForPlugins();
    }
}