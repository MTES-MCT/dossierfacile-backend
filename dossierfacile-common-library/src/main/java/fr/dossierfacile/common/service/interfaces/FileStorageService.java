package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.shared.StoredFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.List;

public interface FileStorageService {
    void delete(StorageFile storageFile);

    void deleteAll(List<StorageFile> storageFiles);

    /**
     * Get the downloaded file's inputStream.
     * If {@code key} is null then the inputStream is directly returned without decrypt operation.
     *
     * @param storageFile StorageFile (path and key) in the used storage
     * @return Encoded (or clear) inputStream
     * @throws IOException If something wrong happen during the download process.
     */
    InputStream download(StorageFile storageFile) throws IOException;

    /**
     * Get the downloaded file's inputStream.
     * Using decrypt information inside the File object if present.
     */
    InputStream download(StoredFile file) throws IOException;

    void upload(String ovhPath, InputStream inputStream, Key key) throws IOException;


    StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException;
    String uploadFile(MultipartFile file, Key key);

    /*
     * Use Storagefile to have the provider
     */
    @Deprecated
    InputStream download(String filepath, Key key) throws IOException;
    @Deprecated
    void delete(String filename);
    @Deprecated
    void delete(List<String> filenames);

}