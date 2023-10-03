package fr.dossierfacile.api.pdfgenerator.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeatureFlipping {
    @Value("${pdf.generation.use.colors:false}")
    private boolean useColors;

    @Value("${pdf.generation.use.distortion:false}")
    private boolean useDistortion;

    @Bean
    public boolean shouldUseColors() {
        return useColors;
    }

    @Bean
    public boolean shouldUseDistortion() {
        return useDistortion;
    }
}
