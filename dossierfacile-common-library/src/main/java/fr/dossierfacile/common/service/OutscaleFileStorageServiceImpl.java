package fr.dossierfacile.common.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import fr.dossierfacile.common.config.ThreeDSOutscaleConfig;
import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.exceptions.UnsupportedKeyException;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.common.service.model.BulkDeleteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service("outscaleFileStorageProvider")
@RequiredArgsConstructor
@Slf4j
@Profile("!mockOvh")
@Deprecated
public class OutscaleFileStorageServiceImpl implements FileStorageProviderService {

    @Autowired
    private ThreeDSOutscaleConfig threeDSOutscaleConfig;

    @Value("${threeds.s3.bucket:dossierfacile-preprod}")
    private String bucket;

    private static InputStream cipherInputStream(String path, EncryptionKey key, InputStream in) throws IOException {
        try {
            Cipher aes;
            if (key.getVersion() == 1 || key.getVersion() == 2) {
                byte[] iv = DigestUtils.sha256(path); // arbitrary set the filename to build IV
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);
            } else {
                throw new UnsupportedKeyException("Unsupported Key version " + key.getVersion());
            }
            in = new CipherInputStream(in, aes);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return in;
    }

    @Override
    public ObjectStorageProvider getProvider() {
        return ObjectStorageProvider.THREEDS_OUTSCALE;
    }

    @Override
    public void delete(String name) {
        AmazonS3 s3client = threeDSOutscaleConfig.getAmazonS3Client();
        s3client.deleteObject(bucket, name);
    }

    /**
     * Bulk delete multiple objects using AWS S3 deleteObjects API.
     * Supports up to 1000 objects per request.
     *
     * @param paths List of object paths to delete
     * @return BulkDeleteResult with successful and failed deletions
     */
    @Override
    public BulkDeleteResult bulkDelete(List<String> paths) {
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
            AmazonS3 s3Client = threeDSOutscaleConfig.getAmazonS3Client();

            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucket)
                    .withKeys(paths.toArray(new String[0]))
                    .withQuiet(false);

            DeleteObjectsResult result = s3Client.deleteObjects(deleteRequest);

            // Track successful deletions
            if (result.getDeletedObjects() != null) {
                for (DeleteObjectsResult.DeletedObject deleted : result.getDeletedObjects()) {
                    successfulPaths.add(deleted.getKey());
                }
            }

            // Objects not in the deleted list might not have existed (which is ok)
            for (String path : paths) {
                if (!successfulPaths.contains(path) && !failedPaths.containsKey(path)) {
                    successfulPaths.add(path);
                }
            }

            log.info("Outscale bulk delete completed: {} successful, {} failed out of {} requested",
                    successfulPaths.size(), failedPaths.size(), paths.size());

            return new BulkDeleteResult(successfulPaths, failedPaths);

        } catch (MultiObjectDeleteException e) {
            // Partial failure - some objects deleted, some failed
            if (e.getDeletedObjects() != null) {
                for (DeleteObjectsResult.DeletedObject deleted : e.getDeletedObjects()) {
                    successfulPaths.add(deleted.getKey());
                }
            }
            if (e.getErrors() != null) {
                for (MultiObjectDeleteException.DeleteError error : e.getErrors()) {
                    failedPaths.put(error.getKey(), error.getCode() + ": " + error.getMessage());
                }
            }

            log.warn("Outscale bulk delete partial failure: {} successful, {} failed",
                    successfulPaths.size(), failedPaths.size());

            return new BulkDeleteResult(successfulPaths, failedPaths);

        } catch (AmazonServiceException e) {
            log.error("Outscale bulk delete failed: {}", e.getMessage(), e);
            return BulkDeleteResult.allFailed(paths, "Outscale error: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String path, EncryptionKey key) throws IOException {
        AmazonS3 s3client = threeDSOutscaleConfig.getAmazonS3Client();
        S3Object fullObject;
        try {
            fullObject = s3client.getObject(new GetObjectRequest(bucket, path));
            InputStream in = fullObject.getObjectContent();
            if (key != null) {
                in = cipherInputStream(path, key, in);
            }
            return in;
        } catch (AmazonServiceException e) {
            throw new FileNotFoundException("File " + path + " not found");
        }
    }

    @Override
    public void upload(String name, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException {
        if (key != null) {
            if (key.getVersion() != 1 && key.getVersion() != 2) {
                throw new UnsupportedKeyException("Unsupported key version " + key.getVersion());
            }
            try {
                byte[] iv = DigestUtils.sha256(name);
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.ENCRYPT_MODE, key, gcmParamSpec);

                inputStream = new CipherInputStream(inputStream, aes);

            } catch (Exception e) {
                log.error("Unable to encrypt file", e);
                throw new RetryableOperationException("Unable to encrypt file", e);
            }
        }

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(contentType);

        AmazonS3 s3Client = threeDSOutscaleConfig.getAmazonS3Client();
        boolean isAccessible;
        try {
            isAccessible = s3Client.doesBucketExistV2(bucket);
        } catch (Exception e) {
            throw new RetryableOperationException("Bucket on Outscale accessibility cannot be reached", e);
        }
        if (!isAccessible) {
            throw new RetryableOperationException("Bucket on Outscale is not accessible");
        }

        PutObjectResult putObjectResult = s3Client.putObject(
                bucket,
                name,
                inputStream,
                objectMetadata);
        if (StringUtils.isEmpty(putObjectResult.getETag())) {
            throw new IOException("ETag is empty - download failed!" + name);
        }
    }

    @Override
    public List<String> listObjectNames(String marker, int maxObjects) {
        AmazonS3 s3Client = threeDSOutscaleConfig.getAmazonS3Client();
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(bucket)
                .withMarker(marker)
                .withMaxKeys(maxObjects);
        return s3Client.listObjects(request)
                .getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey)
                .toList();
    }

    @Override
    public void uploadV2(S3Bucket s3Bucket, String fileKey, InputStream inputStream, String contentType, EncryptionKey key) throws RetryableOperationException, IOException {
        throw new NotImplementedException();
    }

    @Override
    public InputStream downloadV2(S3Bucket bucket, String path, EncryptionKey key) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void deleteV2(S3Bucket bucket, String path) {
        throw new NotImplementedException("OVH does not support deleteV2 operation");
    }

}