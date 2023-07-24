package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ProviderNotFoundException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Profile("!mockOvh")
public class FileStorageServiceImpl implements FileStorageService {
    private final FileStorageProviderService ovhFileStorageService;
    private final FileStorageProviderService outscaleFileStorageService;
    private final StorageFileRepository storageFileRepository;
    @Value("#{'${storage.provider.list}'.split(',')}")
    private List<ObjectStorageProvider> providers;

    public FileStorageServiceImpl(@Qualifier("ovhFileStorageProvider") FileStorageProviderService ovhFileStorageService,
                                  @Qualifier("outscaleFileStorageProvider") FileStorageProviderService outscaleFileStorageService,
                                  StorageFileRepository storageFileRepository) {
        this.ovhFileStorageService = ovhFileStorageService;
        this.outscaleFileStorageService = outscaleFileStorageService;
        this.storageFileRepository = storageFileRepository;
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
        for (ObjectStorageProvider provider : providers) {
            try {
                getStorageService(provider).delete(storageFile.getPath());
            } catch (Exception e) {
                log.warn("Provider " + provider + " to delete file " + storageFile.getPath() + " Failed .", e);
            }
        }
    }

    @Override
    public InputStream download(StorageFile storageFile) throws IOException {
        for (ObjectStorageProvider provider : providers) {
            try {
                return getStorageService(provider)
                        .download(storageFile.getPath(), storageFile.getEncryptionKey());
            } catch (Exception e) {
                log.info("Provider " + provider + " to get file " + storageFile.getPath() + " Failed .");
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(inputStream, baos);
        byte[] bytes = baos.toByteArray();

        // TODO : maybe try to improve velocity
        for (ObjectStorageProvider provider : providers) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                getStorageService(provider)
                        .upload(storageFile.getPath(), bais, storageFile.getEncryptionKey(), storageFile.getContentType());

                storageFile.setProvider(provider);
            } catch (RetryableOperationException e) {
                log.warn("Provider " + provider + " Failed .", e);
            }
        }
        if (storageFile.getProvider() == null)
            throw new IOException("Unable to upload the file");

        return storageFileRepository.save(storageFile);
    }

}