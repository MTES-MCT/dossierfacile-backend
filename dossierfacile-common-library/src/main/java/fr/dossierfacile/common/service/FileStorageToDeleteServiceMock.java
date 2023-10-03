package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.StorageFileToDelete;
import fr.dossierfacile.common.service.interfaces.FileStorageToDeleteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile("mockOvh")
public class FileStorageToDeleteServiceMock implements FileStorageToDeleteService {
    public FileStorageToDeleteServiceMock() {
    }

    @Override
    public void delete(StorageFileToDelete storageFileToDelete) {
        log.warn("Mock version - do nothing");
    }
}