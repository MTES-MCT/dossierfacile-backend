package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.StorageFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.List;

public interface FileStorageService {
    void delete(StorageFile storageFile);

    /**
     * Get the downloaded file's inputStream.
     * If {@code key} is null then the inputStream is directly returned without decrypt operation.
     *
     * @param storageFile StorageFile (path and key) in the used storage
     * @return Encoded (or clear) inputStream
     * @throws IOException If something wrong happen during the download process.
     */
    InputStream download(StorageFile storageFile) throws IOException;

    /*
     * Use Storagefile to have the provider
     */
    @Deprecated
    InputStream download(String filepath, Key key) throws IOException;

    StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException;

    @Deprecated
    void delete(String filename);

    @Deprecated
    void delete(List<String> filenames);

    void upload(String name, InputStream inputStream, Key key, String contentType) throws IOException;
}