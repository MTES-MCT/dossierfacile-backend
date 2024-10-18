package fr.dossierfacile.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageMagickConfig {
    @Value("${path.imagemagick.cli:/usr/bin/convert}")
    private String imageMagickCli;

    public String getImageMagickCli() {
        return imageMagickCli;
    }
}
