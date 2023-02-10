package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.shared.StoredFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.List;

public interface FileStorageService {
    void delete(String filename);

    void delete(List<String> filenames);

    void deleteAllFiles(String path);

    /**
     * Get the downloaded file's inputStream.
     * If {@code key} is null then the inputStream is directly returned without decrypt operation.
     *
     * @param filepath path in the used storage
     * @param key      Encryption key - can be {@code null}
     * @return Encoded (or clear) inputStream
     * @throws IOException If something wrong happen during the download process.
     */
    InputStream download(String filepath, Key key) throws IOException;

    /**
     * Get the downloaded file's inputStream.
     * Using decrypt information inside the File object if present.
     */
    InputStream download(StoredFile file) throws IOException;

    void upload(String ovhPath, InputStream inputStream, Key key) throws IOException;


    String uploadFile(MultipartFile file, Key key);

    StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException;
}