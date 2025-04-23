package fr.dossierfacile.process.file.fileAnalysis;

import fr.dossierfacile.fileAnalysis.S3Configuration;
import fr.dossierfacile.fileAnalysis.TestOvhFileStorageServiceImpl;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.BlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.FFTBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.LaplacianBlurryAlgorithm;
import fr.dossierfacile.process.file.service.processors.blurry.algorithm.SobelBlurryAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TestProperties {
    @Value("${s3.endpoint}")
    private String s3Endpoint;
    @Value("${s3.account.id}")
    private String s3AccountId;
    @Value("${s3.access.key}")
    private String s3AccessKey;
    @Value("${s3.secret.key}")
    private String s3SecretKey;
    @Value("${s3.region}")
    private String s3Region;
    @Value("${s3.bucket.name}")
    private String s3BucketName;
    @Value("${opencv.lib.path}")
    private String opencvLibPath;

    @Autowired
    private LaplacianBlurryAlgorithm laplacianBlurryAlgorithm;
    @Autowired
    private SobelBlurryAlgorithm sobelBlurryAlgorithm;
    @Autowired
    private FFTBlurryAlgorithm fftBlurryAlgorithm;

    @Bean
    TestOvhFileStorageServiceImpl getOvhFileStorageService() {
        return new TestOvhFileStorageServiceImpl(
                new S3Configuration(
                        s3Endpoint,
                        s3AccountId,
                        s3AccessKey,
                        s3SecretKey,
                        s3Region,
                        s3BucketName
                )
        );
    }

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
                fftBlurryAlgorithm
        );
    }
}

record TestOpenCvConfig(String libPath) {
}
