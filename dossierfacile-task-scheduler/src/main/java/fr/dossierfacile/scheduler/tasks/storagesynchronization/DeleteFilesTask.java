package fr.dossierfacile.scheduler.tasks.storagesynchronization;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.scheduler.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.dossierfacile.scheduler.tasks.TaskName.STORAGE_FILES_DELETION;
import static fr.dossierfacile.scheduler.tasks.TaskName.STORAGE_FILES_DELETION_RETRY;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteFilesTask {

    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;

    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.delay.ms}", initialDelayString = "${scheduled.process.storage.delete.delay.ms}")
    public void deleteFileInProviderTask() {
        LoggingContext.startTask(STORAGE_FILES_DELETION);
        Pageable limit = PageRequest.of(0, 500);
        List<StorageFile> storageFileToDeleteList = storageFileRepository.findAllByStatusOrderByIdAsc(FileStorageStatus.TO_DELETE, limit);
        for (StorageFile storageFileToDelete : storageFileToDeleteList) {
            fileStorageService.hardDelete(storageFileToDelete);
        }
        LoggingContext.endTask();
    }

    @Scheduled(fixedDelayString = "${scheduled.process.storage.delete.retry.failed.delay.minutes}", initialDelayString = "${scheduled.process.storage.delete.retry.failed.delay.minutes}", timeUnit = TimeUnit.MINUTES)
    public void retryDeleteFileInProviderTask() {
        LoggingContext.startTask(STORAGE_FILES_DELETION_RETRY);
        Pageable limit = PageRequest.of(0, 1000);
        List<StorageFile> storageFileToDeleteList = storageFileRepository.findAllByStatusOrderByIdAsc(FileStorageStatus.DELETE_FAILED, limit);
        for (StorageFile storageFileToDelete : storageFileToDeleteList) {
            fileStorageService.hardDelete(storageFileToDelete);
        }
        LoggingContext.endTask();
    }

}
