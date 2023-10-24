package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.RetryableOperationException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.List;

public interface FileStorageProviderService {

    ObjectStorageProvider getProvider();

    void delete(String name);

    InputStream download(String path, Key key) throws IOException;

    void upload(String ovhPath, InputStream inputStream, Key key, String contentType) throws RetryableOperationException, IOException;

    List<String> listObjectNames(@Nullable String marker, int maxObjects);

}
