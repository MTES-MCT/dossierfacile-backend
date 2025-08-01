package fr.dossierfacile.common.service;

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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Service("s3FileStorageProvider")
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

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        log.info("S3FileStorageService initialized with region: {}, endpoint: {}", region, endpointUrl);
        // We need to set the properties to avoid using the default AWS config files
        System.setProperty("aws.configFile", "/dev/null");
        System.setProperty("aws.sharedCredentialsFile", "/dev/null");

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpointUrl))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                        )
                )
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .build())
                .build();
    }

    @Override
    public ObjectStorageProvider getProvider() {
        return ObjectStorageProvider.S3;
    }


    @Override
    public InputStream downloadV2(S3Bucket bucket, String path) throws IOException {
        try {
            return s3Client.getObject(getObjectRequest ->
                    getObjectRequest.bucket(bucketMapping.get(bucket)).key(path).build()
            );
        } catch (NoSuchKeyException e) {
            log.error("Error downloading file from S3 bucket: {} with key {}", bucket, path, e);
            throw new IOException("Failed to download file from S3 bucket", e);
        } catch (SdkClientException | S3Exception e) {
            log.error("Error downloading file from S3 bucket: {}", bucket, e);
            throw new IOException("Failed to download file from S3 bucket", e);
        }
    }

    @Override
    public void uploadV2(S3Bucket s3Bucket, String fileKey, InputStream inputStream, String contentType) throws RetryableOperationException {
        try {
            byte[] bytes = inputStream.readAllBytes();
            var putRequest = PutObjectRequest.builder()
                    .bucket(bucketMapping.get(s3Bucket))
                    .key(fileKey)
                    .metadata(Map.of("x-delete-after", "1"))
                    .contentType(contentType)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
        } catch (SdkClientException e) {
            log.error("Error uploading file to S3 bucket: {}", e.getMessage());
            throw new RetryableOperationException("Failed to upload file to S3 bucket", e);
        } catch (IOException e) {
            log.error("Impossible to read input stream for upload: {}", e.getMessage());
        }
    }

    @Override
    public void deleteV2(S3Bucket bucket, String path) throws IOException {
        try {
            s3Client.deleteObject(deleteObjectRequest ->
                    deleteObjectRequest.bucket(bucketMapping.get(bucket)).key(path).build()
            );
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