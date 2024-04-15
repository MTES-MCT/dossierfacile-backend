package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.StorageFileToDelete;

public interface FileStorageToDeleteService {

    void delete(StorageFileToDelete storageFileToDelete);

}