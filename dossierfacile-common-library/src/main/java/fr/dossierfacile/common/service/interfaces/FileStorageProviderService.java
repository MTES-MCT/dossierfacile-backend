package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.service.model.BulkDeleteResult;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FileStorageProviderService {

    ObjectStorageProvider getProvider();

    @Deprecated
    void delete(String name);

    void deleteV2(S3Bucket bucket, String path) throws IOException;

    /**
     * Bulk delete multiple objects from storage.
     * This is the legacy method for deprecated providers (OVH, Outscale).
     *
     * @param paths List of object paths to delete
     * @return BulkDeleteResult containing successful and failed deletions
     */
    @Deprecated
    BulkDeleteResult bulkDelete(List<String> paths);

    /**
     * Bulk delete multiple objects from a specific S3 bucket.
     *
     * @param bucket The S3 bucket
     * @param paths List of object paths to delete
     * @return BulkDeleteResult containing successful and failed deletions
     */
    BulkDeleteResult bulkDeleteV2(S3Bucket bucket, List<String> paths);

    @Deprecated
    InputStream download(String path, EncryptionKey key) throws IOException;

    InputStream downloadV2(S3Bucket bucket, String path, EncryptionKey key) throws IOException;

    @Deprecated
    void upload(String ovhPath, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException;

    void uploadV2(S3Bucket s3Bucket, String fileKey, InputStream inputStream, String contentType, EncryptionKey key) throws RetryableOperationException, IOException;

    List<String> listObjectNames(@Nullable String marker, int maxObjects);

    List<String> listObjectNamesV2(S3Bucket s3Bucket, String prefix);

}
