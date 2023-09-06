package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.StorageFileToDelete;
import fr.dossierfacile.common.exceptions.RetryableOperationException;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageToDeleteService {

    void delete(StorageFileToDelete storageFileToDelete);

}