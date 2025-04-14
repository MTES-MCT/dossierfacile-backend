package fr.dossierfacile.process.file.fileAnalysis;

import fr.dossierfacile.fileAnalysis.S3Configuration;
import fr.dossierfacile.fileAnalysis.TestOvhFileStorageServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OvhConfiguration {
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
}
