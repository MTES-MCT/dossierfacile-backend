package fr.dossierfacile.scheduler.tasks.storagesynchronization;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteFilesTask extends AbstractTask {

    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;

    private void deleteFilesForProvider(ObjectStorageProvider provider, int limit) {
        super.startTask(TaskName.STORAGE_FILES_DELETION);
        try {
            log.info("Deleting files for provider {} with limit {}", provider.name(), limit);
            Pageable pageable = PageRequest.of(0, limit);
            List<StorageFile> storageFileToDeleteList = storageFileRepository.findAllByStatusAndProviderOrderByIdAsc(
                    FileStorageStatus.TO_DELETE,
                    provider,
                    pageable
            );
            addFileIdListForLogging(storageFileToDeleteList);
            for (StorageFile storageFileToDelete : storageFileToDeleteList) {
                fileStorageService.hardDelete(storageFileToDelete);
            }
        } catch (Exception e) {
            log.error("Error during deleteFileInProviderTask for provider {}: {}", provider.name(), e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    // S3 Provider Task
    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.delay.ms}", initialDelayString = "${scheduled.process.storage.delete.delay.ms}")
    public void deleteFilesS3Task() {
        deleteFilesForProvider(ObjectStorageProvider.S3, 1000);
    }

    // TODO: OVH provider is deprecated - remove this task once all OVH files have been migrated and deleted
    // OVH Provider Task
    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.delay.ms}", initialDelayString = "${scheduled.process.storage.delete.delay.ms}")
    public void deleteFilesOvhTask() {
        deleteFilesForProvider(ObjectStorageProvider.OVH, 1000);
    }

    // TODO: THREEDS_OUTSCALE provider is deprecated - remove this task once all Outscale files have been migrated and deleted
    // 3DS Outscale Provider Task
    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.delay.ms}", initialDelayString = "${scheduled.process.storage.delete.delay.ms}")
    public void deleteFilesOutscaleTask() {
        deleteFilesForProvider(ObjectStorageProvider.THREEDS_OUTSCALE, 1000);
    }

    // Generic retry task for all providers
    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.retry.failed.delay.minutes}", initialDelayString = "${scheduled.process.storage.delete.retry.failed.delay.minutes}", timeUnit = TimeUnit.MINUTES)
    public void retryDeleteFileInProviderTask() {
        super.startTask(TaskName.STORAGE_FILES_DELETION_RETRY);
        try {
            Pageable limit = PageRequest.of(0, 1000);
            List<StorageFile> storageFileToDeleteList = storageFileRepository.findAllByStatusOrderByIdAsc(FileStorageStatus.DELETE_FAILED, limit);
            addFileIdListForLogging(storageFileToDeleteList);
            for (StorageFile storageFileToDelete : storageFileToDeleteList) {
                fileStorageService.hardDelete(storageFileToDelete);
            }
        } catch (Exception e) {
            log.error("Error during retryDeleteFailedFilesTask: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

}
