package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.common.service.model.BulkDeleteResult;
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
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service("s3FileStorageProvider")
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageServiceImpl implements FileStorageProviderService {

    @Value("${s3.region:sbg}")
    private String region;
    @Value("${s3.endpoint.url:https://test.fr}")
    private String endpointUrl;
    @Value("${s3.access.key:key}")
    private String accessKeyId;
    @Value("${s3.secret.access.key:secretKey}")
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
    public InputStream downloadV2(S3Bucket bucket, String path, EncryptionKey key) throws IOException {
        try {
            return s3Client.getObject(getObjectRequest(bucket, path, key));
        } catch (NoSuchKeyException e) {
            log.error("Error downloading file from S3 bucket: {} with key {}", bucket, path, e);
            throw new IOException("Failed to download file from S3 bucket", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("No such algorithm for encryption key: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (SdkClientException | S3Exception e) {
            log.error("Error downloading file from S3 bucket: {}", bucket, e);
            throw new IOException("Failed to download file from S3 bucket", e);
        }
    }

    @Override
    public void uploadV2(S3Bucket s3Bucket, String fileKey, InputStream inputStream, String contentType, EncryptionKey key) throws RetryableOperationException {
        try {
            byte[] bytes = inputStream.readAllBytes();
            s3Client.putObject(putObjectRequest(s3Bucket, fileKey, contentType, key), RequestBody.fromBytes(bytes));
        } catch (SdkClientException e) {
            log.error("Error uploading file to S3 bucket: {}", e.getMessage());
            throw new RetryableOperationException("Failed to upload file to S3 bucket", e);
        } catch (NoSuchAlgorithmException e) {
            log.error("No such algorithm for encryption key: {}", e.getMessage());
            throw new RuntimeException(e);
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
    public List<String> listObjectNamesV2(S3Bucket s3Bucket, String prefix) {
        try {
            return s3Client.listObjects(listObjectRequest ->
                    listObjectRequest.bucket(bucketMapping.get(s3Bucket)).prefix(prefix).build()
            ).contents().stream().map(S3Object::key).toList();
        } catch (SdkClientException e) {
            log.error("Error listing objects from S3 bucket: {}", e.getMessage());
            throw new RuntimeException("Failed to list objects from S3 bucket", e);
        }
    }

    /**
     * Bulk delete multiple objects from S3 with detailed error tracking.
     * S3 supports up to 1000 objects per delete request.
     *
     * @param s3Bucket The target bucket
     * @param paths List of object keys to delete
     * @return BulkDeleteResult with successful and failed deletions
     */
    @Override
    public BulkDeleteResult bulkDeleteV2(S3Bucket s3Bucket, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return BulkDeleteResult.empty();
        }

        // S3 bulk delete limit is 1000 objects per request
        if (paths.size() > 1000) {
            log.warn("Bulk delete request exceeds S3 limit of 1000 objects. Processing first 1000 only.");
            paths = paths.subList(0, 1000);
        }

        Set<String> successfulPaths = new HashSet<>();
        Map<String, String> failedPaths = new HashMap<>();

        try {
            List<ObjectIdentifier> objectsToDelete = paths.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();

            Delete delete = Delete.builder()
                    .objects(objectsToDelete)
                    .quiet(false) // We want detailed response
                    .build();

            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketMapping.get(s3Bucket))
                    .delete(delete)
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(deleteRequest);

            // Track successful deletions
            if (response.deleted() != null) {
                for (DeletedObject deleted : response.deleted()) {
                    successfulPaths.add(deleted.key());
                }
            }

            // Track failed deletions
            if (response.errors() != null) {
                for (S3Error error : response.errors()) {
                    String errorMessage = error.code() + ": " + error.message();
                    failedPaths.put(error.key(), errorMessage);
                }
            }

            // Objects not in either list might not exist (which is ok for delete)
            Set<String> processedPaths = new HashSet<>();
            processedPaths.addAll(successfulPaths);
            processedPaths.addAll(failedPaths.keySet());

            for (String path : paths) {
                if (!processedPaths.contains(path)) {
                    // Object wasn't in response - likely didn't exist, consider it successful
                    successfulPaths.add(path);
                }
            }

            log.info("S3 bulk delete completed for bucket {}: {} successful, {} failed out of {} requested",
                    s3Bucket, successfulPaths.size(), failedPaths.size(), paths.size());

            return new BulkDeleteResult(successfulPaths, failedPaths);

        } catch (S3Exception e) {
            // Server-side errors (permissions, bucket not found, etc.)
            log.error("S3 bulk delete server error for bucket {}: {} - {}", s3Bucket, e.awsErrorDetails().errorCode(), e.getMessage(), e);
            return BulkDeleteResult.allFailed(paths, "S3 server error: " + e.awsErrorDetails().errorCode() + " - " + e.getMessage());
        } catch (SdkClientException e) {
            // Client-side errors (network, configuration, etc.)
            log.error("S3 bulk delete client error for bucket {}: {}", s3Bucket, e.getMessage(), e);
            return BulkDeleteResult.allFailed(paths, "S3 client error: " + e.getMessage());
        }
    }

    @Override
    public List<String> listObjectNames(@Nullable String marker, int maxObjects) {
        throw new NotImplementedException("S3 does not support listObjectNames operation");
    }

    @Override
    public void upload(String path, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException {
        throw new NotImplementedException("S3 does not support upload operation");
    }

    @Override
    public void delete(String path) {
        throw new NotImplementedException("S3 does not support delete operation");
    }

    @Override
    public InputStream download(String path, EncryptionKey key) throws IOException {
        throw new NotImplementedException("S3 does not support download operation");
    }

    @Override
    public BulkDeleteResult bulkDelete(List<String> paths) {
        throw new NotImplementedException("S3 does not support bulkDelete operation");
    }

    private GetObjectRequest getObjectRequest(S3Bucket bucket, String path, EncryptionKey key) throws NoSuchAlgorithmException {
        var builder = GetObjectRequest.builder()
                .bucket(bucketMapping.get(bucket))
                .key(path);

        if (key != null) {
            builder.sseCustomerAlgorithm("AES256");
            builder.sseCustomerKey(getSSEBase64Key(key));
            builder.sseCustomerKeyMD5(getSSEBase64KeyMD5(key));
        }
        return builder.build();
    }

    private PutObjectRequest putObjectRequest(S3Bucket bucket, String fileKey, String contentType, EncryptionKey key) throws NoSuchAlgorithmException {
        var builder = PutObjectRequest.builder()
                .bucket(bucketMapping.get(bucket))
                .key(fileKey)
                .contentType(contentType);

        if (key != null) {
            builder.sseCustomerAlgorithm("AES256");
            builder.sseCustomerKey(getSSEBase64Key(key));
            builder.sseCustomerKeyMD5(getSSEBase64KeyMD5(key));
        }
        return builder.build();

    }

    private String getSSEBase64Key(EncryptionKey key) {
        return Base64.getEncoder().encodeToString(key.getEncodedSecret());
    }

    private String getSSEBase64KeyMD5(EncryptionKey key) throws NoSuchAlgorithmException {
        return Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(key.getEncodedSecret()));
    }
}