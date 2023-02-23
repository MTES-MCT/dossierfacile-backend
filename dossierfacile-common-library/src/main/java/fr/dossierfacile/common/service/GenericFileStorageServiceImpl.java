package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.shared.StoredFile;
import fr.dossierfacile.common.exceptions.FileCannotUploadedException;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ProviderNotFoundException;
import java.security.Key;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@Profile("!mockOvh")
public class GenericFileStorageServiceImpl implements FileStorageService {
    private static final String EXCEPTION = "Sentry ID Exception: ";
    private String tokenId;

    private OvhFileStorageServiceImpl ovhFileStorageService;
    private ThreeDSOutscaleFileStorageServiceImpl threeDSOutscaleFileStorageService;

    @Autowired
    private StorageFileRepository storageFileRepository;

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
        if (ObjectStorageProvider.OVH.equals(storageFile.getProvider())) {
            ovhFileStorageService.delete(storageFile.getPath());
            return;
        } else if (ObjectStorageProvider.THREEDS_OUTSCALE.equals(storageFile.getProvider())) {
            threeDSOutscaleFileStorageService.delete(storageFile.getPath());
            return;
        }
        throw new ProviderNotFoundException();
    }

    @Override
    @Async
    public void deleteAll(List<StorageFile> storageFiles) {
        storageFiles.forEach(this::delete);
    }

    @Override
    @Async
    public void delete(List<String> names) {
        names.forEach(this::delete);
    }

    @Override
    public InputStream download(StorageFile storageFile) throws IOException {
        if (ObjectStorageProvider.OVH.equals(storageFile.getProvider())) {
            return ovhFileStorageService.download(storageFile.getPath(), storageFile.getEncryptionKey());
        } else if (ObjectStorageProvider.THREEDS_OUTSCALE.equals(storageFile.getProvider())) {
            return threeDSOutscaleFileStorageService.download(storageFile.getPath(), storageFile.getEncryptionKey());
        }
        throw new ProviderNotFoundException();
    }

    @Override
    public InputStream download(String path, Key key) throws IOException {
        return ovhFileStorageService.download(path, key);
    }

    @Override
    public InputStream download(StoredFile file) throws IOException {
        return download(file.getPath(), file.getEncryptionKey());
    }

    @Override
    public void upload(String name, InputStream inputStream, Key key) throws IOException {
        // Default to 3DS Outscale but fallback to ovh
        try {
            threeDSOutscaleFileStorageService.upload(name, inputStream, key);
        } catch (Exception e) {
            ovhFileStorageService.upload(name, inputStream, key);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, Key key) {
        String name = UUID.randomUUID() + "." + Objects.requireNonNull(FilenameUtils.getExtension(file.getOriginalFilename())).toLowerCase(Locale.ROOT);
        try (InputStream is = file.getInputStream()) {
            upload(name, is, key);
        } catch (IOException e) {
            throw new FileCannotUploadedException();
        }
        return name;
    }

    @Override
    public StorageFile upload(InputStream inputStream, StorageFile storageFile) throws IOException {
        if (inputStream == null)
            return null;
        if (storageFile == null) {
            log.warn("fallback on uploadfile");
            storageFile = StorageFile.builder()
                    .name("undefined")
                    .provider(ObjectStorageProvider.OVH)
                    .build();
        }

        if (StringUtils.isBlank(storageFile.getPath())) {
            storageFile.setPath(UUID.randomUUID().toString());
        }
        upload(storageFile.getPath(), inputStream, storageFile.getEncryptionKey());

        return storageFileRepository.save(storageFile);

    }
}