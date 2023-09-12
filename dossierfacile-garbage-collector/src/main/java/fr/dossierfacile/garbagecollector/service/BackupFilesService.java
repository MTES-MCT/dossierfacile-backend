package fr.dossierfacile.garbagecollector.service;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupFilesService {
    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;

    @Scheduled(fixedDelay = 5000)
    public void scheduleBackupTask() {
        Pageable limit = PageRequest.of(0, 100);
        List<StorageFile> storageFiles = storageFileRepository.findAllWithOneProvider(limit);
        for (StorageFile storageFile : storageFiles) {
            for (ObjectStorageProvider objectStorageProvider : ObjectStorageProvider.values()) {
                if (!storageFile.getProviders().contains(objectStorageProvider.name())) {
                    try (InputStream is = fileStorageService.download(storageFile)) {
                        fileStorageService.uploadToProvider(is, storageFile, objectStorageProvider);
                    } catch (Exception e) {
                        log.error("Upload to " + objectStorageProvider + " failed for storage_file with id : " + storageFile.getId());
                    }
                }
            }
        }
    }
}
