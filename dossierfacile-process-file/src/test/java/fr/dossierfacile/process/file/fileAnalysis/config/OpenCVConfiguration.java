package fr.dossierfacile.process.file.fileAnalysis.config;

import fr.dossierfacile.process.file.service.processors.blurry.algorithm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenCVConfiguration {
    @Value("${opencv.lib.path}")
    private String opencvLibPath;

    @Autowired
    private LaplacianBlurryAlgorithm laplacianBlurryAlgorithm;
    @Autowired
    private SobelBlurryAlgorithm sobelBlurryAlgorithm;
    @Autowired
    private FFTBlurryAlgorithm fftBlurryAlgorithm;
    @Autowired
    private DifferenceOfGaussiansBlurryAlgorithm differenceOfGaussiansBlurryAlgorithm;

    @Bean
    TestOpenCvConfig getOpenCvConfig() {
        return new TestOpenCvConfig(
                opencvLibPath
        );
    }

    @Bean
    List<BlurryAlgorithm> getBlurryAlgorithms() {
        return List.of(
                laplacianBlurryAlgorithm,
                sobelBlurryAlgorithm,
                fftBlurryAlgorithm,
                differenceOfGaussiansBlurryAlgorithm
        );
    }
}

record TestOpenCvConfig(String libPath) {
}
