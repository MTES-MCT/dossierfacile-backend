package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.exceptions.RetryableOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;

public interface FileStorageProviderService {
    void delete(String name);

    InputStream download(String path, Key key) throws IOException;

    void upload(String ovhPath, InputStream inputStream, Key key, String contentType) throws RetryableOperationException, IOException;
}
