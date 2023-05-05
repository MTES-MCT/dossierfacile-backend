package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.StorageFile;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.List;

public interface StorageProviderService {
    @Async
    void delete(String name);

    @Async
    void delete(List<String> name);

    InputStream download(String path, Key key) throws IOException;

    StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException;

    void upload(String ovhPath, InputStream inputStream, Key key, String contentType) throws IOException;
}
