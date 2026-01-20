package fr.dossierfacile.scheduler.tasks.storagesynchronization;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.FileBatchDeletionService;
import fr.dossierfacile.common.service.FileBatchDeletionService.BatchDeletionResult;
import fr.dossierfacile.scheduler.tasks.AbstractTask;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled task for deleting files from object storage and database.
 * Uses batch/bulk deletion for improved performance.
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Bulk delete on storage providers (1000/request)</li>
 *   <li>Batch delete on database</li>
 *   <li>Robust error handling with partial failure support</li>
 *   <li>Failed deletions are marked as DELETE_FAILED for retry</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteFilesTask extends AbstractTask {

    private final StorageFileRepository storageFileRepository;
    private final FileBatchDeletionService fileBatchDeletionService;

    @Value("${scheduled.process.storage.delete.batch.size:1000}")
    private int batchSize;

    /**
     * Delete files for a specific provider using batch operations.
     *
     * @param provider The storage provider to process
     * @param limit Maximum number of files to process in this execution
     */
    private void deleteFilesForProvider(ObjectStorageProvider provider, int limit) {
        super.startTask(TaskName.STORAGE_FILES_DELETION);
        try {
            // Log backlog size for monitoring
            long backlogCount = storageFileRepository.countByStatusAndProvider(
                    FileStorageStatus.TO_DELETE, provider);
            log.info("Starting batch deletion for provider {} - backlog: {} files, processing up to {}",
                    provider.name(), backlogCount, limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<StorageFile> storageFileToDeleteList = storageFileRepository.findAllByStatusAndProviderOrderByIdAsc(
                    FileStorageStatus.TO_DELETE,
                    provider,
                    pageable
            );

            if (storageFileToDeleteList.isEmpty()) {
                log.debug("No files to delete for provider {}", provider.name());
                return;
            }

            addFileIdListForLogging(storageFileToDeleteList);

            // Use batch deletion service for bulk operations
            BatchDeletionResult result = fileBatchDeletionService.deleteBatch(storageFileToDeleteList);

            log.info("Batch deletion completed for provider {}: {} processed, {} deleted, {} failed",
                    provider.name(),
                    result.totalProcessed(),
                    result.successfullyDeleted(),
                    result.markedAsFailed());

        } catch (Exception e) {
            log.error("Error during deleteFileInProviderTask for provider {}: {}", provider.name(), e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    /**
     * S3 Provider Task - runs every N seconds (configurable).
     * S3 bulk delete supports up to 1000 objects per request.
     */
    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.delay.ms}",
            initialDelayString = "${scheduled.process.storage.delete.delay.ms}")
    public void deleteFilesS3Task() {
        deleteFilesForProvider(ObjectStorageProvider.S3, batchSize);
    }

    /**
     * OVH Provider Task - runs every N seconds (configurable).
     * OVH Swift bulk delete supports up to 10,000 objects per request.
     *
     * @deprecated OVH provider is deprecated - remove this task once all OVH files have been migrated and deleted
     */
    @Deprecated
    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.delay.ms}",
            initialDelayString = "${scheduled.process.storage.delete.delay.ms}")
    public void deleteFilesOvhTask() {
        deleteFilesForProvider(ObjectStorageProvider.OVH, batchSize);
    }

    /**
     * 3DS Outscale Provider Task - runs every N seconds (configurable).
     *
     * @deprecated THREEDS_OUTSCALE provider is deprecated - remove this task once all Outscale files have been migrated and deleted
     */
    @Deprecated
    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.delay.ms}",
            initialDelayString = "${scheduled.process.storage.delete.delay.ms}")
    public void deleteFilesOutscaleTask() {
        deleteFilesForProvider(ObjectStorageProvider.THREEDS_OUTSCALE, batchSize);
    }

    /**
     * Generic retry task for all providers.
     * Retries files that previously failed to delete.
     * Uses individual deletion for better error isolation during retry.
     */
    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.retry.failed.delay.minutes}",
            initialDelayString = "${scheduled.process.storage.delete.retry.failed.delay.minutes}",
            timeUnit = TimeUnit.MINUTES)
    public void retryDeleteFileInProviderTask() {
        super.startTask(TaskName.STORAGE_FILES_DELETION_RETRY);
        try {
            Pageable limit = PageRequest.of(0, batchSize);
            List<StorageFile> storageFileToDeleteList = storageFileRepository.findAllByStatusOrderByIdAsc(
                    FileStorageStatus.DELETE_FAILED, limit);

            if (storageFileToDeleteList.isEmpty()) {
                log.debug("No failed files to retry");
                return;
            }

            addFileIdListForLogging(storageFileToDeleteList);
            log.info("Retrying deletion of {} previously failed files", storageFileToDeleteList.size());

            // For retries, we can still use batch deletion
            // Files that fail again will be marked DELETE_FAILED again
            BatchDeletionResult result = fileBatchDeletionService.deleteBatch(storageFileToDeleteList);

            log.info("Retry batch completed: {} processed, {} deleted, {} still failing",
                    result.totalProcessed(),
                    result.successfullyDeleted(),
                    result.markedAsFailed());

        } catch (Exception e) {
            log.error("Error during retryDeleteFailedFilesTask: {}", e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

}
