package fr.dossierfacile.common.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service("S3FileStorageProvider")
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageServiceImpl implements FileStorageProviderService {

    @Value("${s3.region:sbg}")
    private String region;
    @Value("${s3.endpoint.url}")
    private String endpointUrl;
    @Value("${s3.access.key}")
    private String accessKeyId;
    @Value("${s3.secret.access.key}")
    private String secretAccessKey;

    private final Map<S3Bucket, String> bucketMapping;

    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        log.info("S3FileStorageService initialized with region: {}, endpoint: {}", region, endpointUrl);
        s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl, region))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey)))
                .build();
    }

    @Override
    public ObjectStorageProvider getProvider() {
        return ObjectStorageProvider.S3;
    }


    @Override
    public InputStream downloadV2(S3Bucket bucket, String path) throws IOException {
        try {
            return s3Client.getObject(
                    new GetObjectRequest(bucketMapping.get(bucket), path)
            ).getObjectContent();
        } catch (SdkClientException e) {
            log.error("Error downloading file from S3 bucket: {}", e.getMessage());
            throw new IOException("Failed to download file from S3 bucket", e);
        }
    }

    @Override
    public void uploadV2(S3Bucket s3Bucket, String fileKey, InputStream inputStream, String contentType) throws RetryableOperationException {
        var metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        try {
            s3Client.putObject(
                    new PutObjectRequest(
                            bucketMapping.get(s3Bucket), fileKey, inputStream, metadata
                    )
            );
        } catch (SdkClientException e) {
            log.error("Error uploading file to S3 bucket: {}", e.getMessage());
            throw new RetryableOperationException("Failed to upload file to S3 bucket", e);
        }
    }

    @Override
    public void deleteV2(S3Bucket bucket, String path) throws IOException {
        try {
            s3Client.deleteObject(bucketMapping.get(bucket), path);
        } catch (SdkClientException e) {
            log.error("Error deleting file from S3 bucket: {}", e.getMessage());
            throw new IOException("Failed to delete file from S3 bucket", e);
        }
    }

    @Override
    public List<String> listObjectNames(@Nullable String marker, int maxObjects) {
        throw new NotImplementedException();
    }

    @Override
    public void upload(String path, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException {
        throw new NotImplementedException();
    }

    @Override
    public void delete(String path) {
        throw new NotImplementedException();
    }

    @Override
    public InputStream download(String path, EncryptionKey key) throws IOException {
        throw new NotImplementedException();
    }

}