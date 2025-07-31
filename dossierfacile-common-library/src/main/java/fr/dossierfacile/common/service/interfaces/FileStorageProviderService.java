package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.model.S3Bucket;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FileStorageProviderService {

    ObjectStorageProvider getProvider();

    @Deprecated
    void delete(String name);

    void deleteV2(S3Bucket bucket, String path) throws IOException;

    @Deprecated
    InputStream download(String path, EncryptionKey key) throws IOException;

    InputStream downloadV2(S3Bucket bucket, String path) throws IOException;

    @Deprecated
    void upload(String ovhPath, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException;

    void uploadV2(S3Bucket s3Bucket, String fileKey, InputStream inputStream, String contentType) throws RetryableOperationException, IOException;

    List<String> listObjectNames(@Nullable String marker, int maxObjects);

}
