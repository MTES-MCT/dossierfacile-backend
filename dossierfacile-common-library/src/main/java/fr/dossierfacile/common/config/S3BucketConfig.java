package fr.dossierfacile.common.config;

import fr.dossierfacile.common.model.S3Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class S3BucketConfig {

    @Value("${s3.bucket.raw.file.name:raw-file}")
    String rawFileBucketName;
    @Value("${s3.bucket.raw.minified.name:raw-minified}")
    String rawMinifiedBucketName;
    @Value("${s3.bucket.watermark.doc.name:watermark-doc}")
    String watermarkDocBucketName;
    @Value("${s3.bucket.full.pdf.name:full-pdf}")
    String fullPdfBucketName;
    @Value("${s3.bucket.filigrane.name:filigrane}")
    String filigraneBucketName;

    @Bean
    public Map<S3Bucket, String> getBucketMapping() {
        return Map.of(
                S3Bucket.RAW_FILE, rawFileBucketName,
                S3Bucket.RAW_MINIFIED, rawMinifiedBucketName,
                S3Bucket.WATERMARK_DOC, watermarkDocBucketName,
                S3Bucket.FULL_PDF, fullPdfBucketName,
                S3Bucket.FILIGRANE, filigraneBucketName
        );
    }

}
