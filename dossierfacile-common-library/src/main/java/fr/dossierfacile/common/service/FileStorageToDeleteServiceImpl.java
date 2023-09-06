package fr.dossierfacile.common.service;

import fr.dossierfacile.common.config.DynamicProviderConfig;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFileToDelete;
import fr.dossierfacile.common.repository.StorageFileToDeleteRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.common.service.interfaces.FileStorageToDeleteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.ProviderNotFoundException;

@Service
@Slf4j
@Profile("!mockOvh")
public class FileStorageToDeleteServiceImpl implements FileStorageToDeleteService {
    private final FileStorageProviderService ovhFileStorageService;
    private final FileStorageProviderService outscaleFileStorageService;
    private final StorageFileToDeleteRepository storageFileToDeleteRepository;
    private final DynamicProviderConfig dynamicProviderConfig;
    public FileStorageToDeleteServiceImpl(@Qualifier("ovhFileStorageProvider") FileStorageProviderService ovhFileStorageService,
                                          @Qualifier("outscaleFileStorageProvider") FileStorageProviderService outscaleFileStorageService,
                                          StorageFileToDeleteRepository storageFileToDeleteRepository, DynamicProviderConfig dynamicProviderConfig) {
        this.ovhFileStorageService = ovhFileStorageService;
        this.outscaleFileStorageService = outscaleFileStorageService;
        this.storageFileToDeleteRepository = storageFileToDeleteRepository;
        this.dynamicProviderConfig = dynamicProviderConfig;
    }

    private FileStorageProviderService getStorageService(ObjectStorageProvider storageProvider) {
        return switch (storageProvider) {
            case OVH -> ovhFileStorageService;
            case THREEDS_OUTSCALE -> outscaleFileStorageService;
            default -> throw new ProviderNotFoundException();
        };
    }

    @Override
    public void delete(StorageFileToDelete storageFileToDelete) {
        if (storageFileToDelete == null) {
            return;
        }
        if (storageFileToDelete.getProviders() == null) {
            storageFileToDeleteRepository.delete(storageFileToDelete);
            return;
        }
        for (String provider : storageFileToDelete.getProviders()) {
            getStorageService(ObjectStorageProvider.valueOf(provider)).delete(storageFileToDelete.getPath());
        }
        storageFileToDeleteRepository.delete(storageFileToDelete);
    }


}