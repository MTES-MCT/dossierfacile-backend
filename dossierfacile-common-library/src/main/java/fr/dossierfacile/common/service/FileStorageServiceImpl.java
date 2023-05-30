package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.OvhFileStorageService;
import fr.dossierfacile.common.service.interfaces.ThreeDSOutscaleFileStorageService;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ProviderNotFoundException;
import java.security.Key;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!mockOvh")
public class FileStorageServiceImpl implements FileStorageService {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    @Autowired
    private OvhFileStorageService ovhFileStorageService;
    @Autowired
    private ThreeDSOutscaleFileStorageService threeDSOutscaleFileStorageService;

    @Autowired
    private StorageFileRepository storageFileRepository;

    @Value("#{'${storage.provider.list}'.split(',')}")
    private List<ObjectStorageProvider> providers;

    @Override
    @Async
    @Deprecated
    /*
      If there is only a path, we suppose that we use the old ovh way
     */
    public void delete(String path) {
        ovhFileStorageService.delete(path);
    }

    @Override
    @Async
    public void delete(StorageFile storageFile) {
        if (storageFile == null) {
            return;
        }
        switch (storageFile.getProvider()) {
            case OVH -> ovhFileStorageService.delete(storageFile.getPath());
            case THREEDS_OUTSCALE -> threeDSOutscaleFileStorageService.delete(storageFile.getPath());
            default -> throw new ProviderNotFoundException();
        }
    }

    @Override
    @Async
    public void delete(List<String> names) {
        names.forEach(this::delete);
    }

    @Override
    public InputStream download(StorageFile storageFile) throws IOException {
        InputStream in;
        switch (storageFile.getProvider()) {
            case OVH -> in = ovhFileStorageService.download(storageFile.getPath(), storageFile.getEncryptionKey());
            case THREEDS_OUTSCALE ->
                    in = threeDSOutscaleFileStorageService.download(storageFile.getPath(), storageFile.getEncryptionKey());
            default -> throw new ProviderNotFoundException();
        }
        return in;
    }

    @Override
    @Deprecated
    public InputStream download(String path, Key key) throws IOException {
        return ovhFileStorageService.download(path, key);
    }

    @Override
    public StorageFile upload(InputStream inputStream, StorageFile storageFile) {
        if (inputStream == null)
            return null;
        if (storageFile == null) {
            log.warn("fallback on uploadfile");
            storageFile = StorageFile.builder()
                    .name("undefined")
                    .build();
        }

        if (StringUtils.isBlank(storageFile.getPath())) {
            storageFile.setPath(UUID.randomUUID().toString());
        }

        for (ObjectStorageProvider provider : providers) {
            try {
                switch (provider) {
                    case OVH -> ovhFileStorageService.upload(inputStream, storageFile);
                    case THREEDS_OUTSCALE -> threeDSOutscaleFileStorageService.upload(inputStream, storageFile);
                    default -> throw new ProviderNotFoundException();
                }
                storageFile.setProvider(provider);
                break;
            } catch (Exception e) {
                log.error("Unable to save to provider : " + provider, e);
                Sentry.captureException(e);
            }
        }

        return storageFileRepository.save(storageFile);

    }

    @Override
    public void upload(String name, InputStream inputStream, Key key, String contentType) throws IOException {
        try {
            ovhFileStorageService.upload(name, inputStream, key, contentType);
        } catch (Exception e) {
            log.error("Unable to save to provider : OVH", e);
            Sentry.captureException(e);
        }
    }

}