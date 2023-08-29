package fr.dossierfacile.common.service;

import fr.dossierfacile.common.config.DynamicProviderConfig;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Profile("!mockOvh")
public class FileStorageServiceImpl implements FileStorageService {
    private final FileStorageProviderService ovhFileStorageService;
    private final FileStorageProviderService outscaleFileStorageService;
    private final StorageFileRepository storageFileRepository;
    private final DynamicProviderConfig dynamicProviderConfig;
    public FileStorageServiceImpl(@Qualifier("ovhFileStorageProvider") FileStorageProviderService ovhFileStorageService,
                                  @Qualifier("outscaleFileStorageProvider") FileStorageProviderService outscaleFileStorageService,
                                  StorageFileRepository storageFileRepository, DynamicProviderConfig dynamicProviderConfig) {
        this.ovhFileStorageService = ovhFileStorageService;
        this.outscaleFileStorageService = outscaleFileStorageService;
        this.storageFileRepository = storageFileRepository;
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
    public void delete(StorageFile storageFile) {
        if (storageFile == null) {
            return;
        }
        storageFileRepository.save(storageFile);
        getStorageService(storageFile.getProvider()).delete(storageFile.getPath());
    }

    @Override
    public InputStream download(StorageFile storageFile) throws IOException {
        List<String> availableProviders = storageFile.getProviders();
        for (ObjectStorageProvider provider : dynamicProviderConfig.getProviders()) {
            Optional<String> selectedProvider = availableProviders.stream().filter(s -> Objects.equals(s, provider.name())).findAny();
            if(selectedProvider.isPresent()) {
                try {
                    return getStorageService(ObjectStorageProvider.valueOf(selectedProvider.get()))
                            .download(storageFile.getPath(), storageFile.getEncryptionKey());
                } catch (Exception e) {
                    log.warn("File " + storageFile.getId() + " was not avalaible in storage : " + provider);
                }
            }
        }
        throw new IOException();
    }

    @Override
    public StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException {
        if (inputStream == null) {
            return null;
        }
        if (storageFile == null) {
            log.warn("fallback to new storage file if file is null");
            storageFile = StorageFile.builder().name("undefined").build();
        }

        if (StringUtils.isBlank(storageFile.getPath())) {
            storageFile.setPath(UUID.randomUUID().toString());
        }

        if (inputStream.markSupported()) {
            inputStream.mark(100000000);
        }
        boolean shift=false;
        for (ObjectStorageProvider provider : dynamicProviderConfig.getProviders()) {
            boolean tryNextProvider = false;
            try {
                storageFile = uploadToProvider(inputStream, storageFile, provider);
            } catch (RetryableOperationException e) {
                log.warn("Provider " + provider + " Failed - Retry with the next provider if exists.", e);
                shift=true;
                if(inputStream.markSupported()) {
                    inputStream.reset();
                    tryNextProvider = true;
                }
            }
            if (!tryNextProvider) {
                break;
            }
        }
        if (shift) {
            dynamicProviderConfig.shift();
        }
        if (storageFile.getProviders() == null || storageFile.getProviders().size() == 0) {
            throw new IOException("Unable to upload the file");
        }

        return storageFileRepository.save(storageFile);
    }

    @Override
    public StorageFile uploadToProvider(InputStream inputStream, StorageFile storageFile, ObjectStorageProvider provider) throws RetryableOperationException, IOException {
        getStorageService(provider)
                .upload(storageFile.getPath(), inputStream, storageFile.getEncryptionKey(), storageFile.getContentType());
        List<String> providers = storageFile.getProviders();
        if (providers == null) {
            providers = new ArrayList<>();
        }
        providers.add(provider.name());
        storageFile.setProviders(providers);
        if (storageFile.getProvider() == null) {
            storageFile.setProvider(provider);
        }
        return storageFileRepository.save(storageFile);
    }

}