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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Task to delete temporary files from deprecated storage providers (OVH and THREEDS_OUTSCALE).
 * These files are no longer needed as the system has migrated to S3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteTemporaryFilesTask extends AbstractTask {

    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;

    @Value("${scheduled.process.storage.delete.temporary.delay.hours:24}")
    private Long delayBeforeDeleteHours;

    private void deleteTemporaryFilesForProvider(ObjectStorageProvider provider, int limit) {
        super.startTask(TaskName.TEMPORARY_FILES_DELETION);
        try {
            LocalDateTime toDateTime = LocalDateTime.now().minusHours(delayBeforeDeleteHours);
            log.info("Deleting temporary files for provider {} older than {} with limit {}", provider.name(), toDateTime, limit);

            Pageable pageable = PageRequest.of(0, limit);
            List<StorageFile> storageFileToDeleteList = storageFileRepository.findAllByStatusAndProviderAndLastModifiedDateBeforeOrderByIdAsc(
                    FileStorageStatus.TEMPORARY,
                    provider,
                    toDateTime,
                    pageable
            );

            if (storageFileToDeleteList.isEmpty()) {
                log.info("No temporary files to delete for provider {}", provider.name());
            } else {
                addFileIdListForLogging(storageFileToDeleteList);
                log.info("Found {} temporary files to delete for provider {}", storageFileToDeleteList.size(), provider.name());
                for (StorageFile storageFileToDelete : storageFileToDeleteList) {
                    fileStorageService.hardDelete(storageFileToDelete);
                }
            }
        } catch (Exception e) {
            log.error("Error during deleteTemporaryFilesForProvider for provider {}: {}", provider.name(), e.getMessage(), e);
        } finally {
            super.endTask();
        }
    }

    // OVH Provider Task - Delete temporary files from deprecated OVH storage
    // Les fichiers avec statut TEMPORARY sont issus de la solution FiligraneFacile
    @Scheduled(cron = "${cron.delete.temporary.files.ovh:0 0 * * * *}")
    public void deleteTemporaryFilesOvhTask() {
        deleteTemporaryFilesForProvider(ObjectStorageProvider.OVH, 1000);
    }

    // S3 Provider Task - Delete temporary files from S3 storage
    // Les fichiers avec statut TEMPORARY sont issus de la solution FiligraneFacile
    @Scheduled(cron = "${cron.delete.temporary.files.s3:0 30 * * * *}")
    public void deleteTemporaryFilesS3Task() {
        deleteTemporaryFilesForProvider(ObjectStorageProvider.S3, 1000);
    }
}
