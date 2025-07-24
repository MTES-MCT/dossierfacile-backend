package fr.dossierfacile.scheduler.tasks.storagesynchronization;

import fr.dossierfacile.common.config.DynamicProviderConfig;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.dossierfacile.scheduler.tasks.TaskName.STORAGE_FILES_BACKUP;
import static fr.dossierfacile.scheduler.tasks.TaskName.STORAGE_FILES_BACKUP_RETRY;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupFilesTask extends AbstractTask {
    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;
    private final DynamicProviderConfig dynamicProviderConfig;

    private static boolean isNotPresentOnProvider(StorageFile storageFile, ObjectStorageProvider objectStorageProvider) {
        return !storageFile.getProviders().contains(objectStorageProvider.name());
    }

    @Scheduled(fixedDelayString = "${scheduled.process.storage.backup.delay.ms}", initialDelayString = "${scheduled.process.storage.backup.delay.ms}")
    public void scheduleBackupTask() {
        super.startTask(STORAGE_FILES_BACKUP);
        Pageable limit = PageRequest.of(0, 100);
        List<StorageFile> storageFiles = storageFileRepository.findAllWithOneProviderAndReady(limit);
        synchronizeFile(storageFiles);
        super.endTask();
    }

    @Scheduled(fixedDelayString = "${scheduled.process.storage.backup.retry.failed.copy.delay.minutes}", initialDelayString = "${scheduled.process.storage.backup.retry.failed.copy.delay.minutes}", timeUnit = TimeUnit.MINUTES)
    public void retryFailedCopy() {
        super.startTask(STORAGE_FILES_BACKUP_RETRY);
        Pageable limit = PageRequest.of(0, 100);
        List<StorageFile> storageFiles = storageFileRepository.findAllWithOneProviderAndCopyFailed(limit);
        synchronizeFile(storageFiles);
        super.endTask();
    }

    private void synchronizeFile(List<StorageFile> storageFiles) {
        addFileIdListForLogging(storageFiles);
        storageFiles.forEach(storageFile -> {
            for (ObjectStorageProvider objectStorageProvider : dynamicProviderConfig.getProviders()) {
                if (isNotPresentOnProvider(storageFile, objectStorageProvider)) {
                    try (InputStream is = fileStorageService.download(storageFile)) {
                        fileStorageService.uploadToProvider(is, storageFile, objectStorageProvider, null);
                    } catch (Exception e) {
                        log.error("Failed copy for {} to {}", storageFile.getId(), objectStorageProvider);
                    }
                }
            }
        });
    }

}
