package fr.dossierfacile.scheduler.tasks.storagesynchronization;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.scheduler.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

import static fr.dossierfacile.scheduler.tasks.TaskName.STORAGE_FILES_BACKUP;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupFilesTask {
    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;

    @Scheduled(fixedDelayString = "${scheduled.process.storage.backup.delay.ms}", initialDelayString = "${scheduled.process.storage.backup.delay.ms}")
    public void scheduleBackupTask() {
        LoggingContext.startTask(STORAGE_FILES_BACKUP);
        Pageable limit = PageRequest.of(0, 100);
        List<StorageFile> storageFiles = storageFileRepository.findAllWithOneProvider(limit);
        storageFiles.parallelStream().forEach(storageFile -> {
            LoggingContext.setStorageFile(storageFile);
            for (ObjectStorageProvider objectStorageProvider : ObjectStorageProvider.values()) {
                if (isNotPresentOnProvider(storageFile, objectStorageProvider)) {
                    uploadFileToProvider(storageFile, objectStorageProvider);
                }
            }
            LoggingContext.clear();
        });
        LoggingContext.endTask();
    }

    private static boolean isNotPresentOnProvider(StorageFile storageFile, ObjectStorageProvider objectStorageProvider) {
        return !storageFile.getProviders().contains(objectStorageProvider.name());
    }

    private void uploadFileToProvider(StorageFile storageFile, ObjectStorageProvider objectStorageProvider) {
        try (InputStream is = fileStorageService.download(storageFile)) {
            fileStorageService.uploadToProvider(is, storageFile, objectStorageProvider);
        } catch (Exception e) {
            log.error("Upload to {} failed", objectStorageProvider);
        }
    }

}
