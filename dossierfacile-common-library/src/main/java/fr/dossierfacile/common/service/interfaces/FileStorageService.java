package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.exceptions.RetryableOperationException;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    /**
     * Soft delete
     */
    void delete(StorageFile storageFile);
    void hardDelete(StorageFile storageFile);
    /**
     * Get the downloaded file's inputStream.
     * If {@code key} is null then the inputStream is directly returned without decrypt operation.
     *
     * @param storageFile StorageFile (path and key) in the used storage
     * @return Encoded (or clear) inputStream
     * @throws IOException If something wrong happen during the download process.
     */
    InputStream download(StorageFile storageFile) throws IOException;

    StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException;

    StorageFile uploadToProvider(InputStream inputStream, StorageFile storageFile, ObjectStorageProvider provider) throws RetryableOperationException, IOException;
}