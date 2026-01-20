package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.common.service.model.BulkDeleteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.ProviderNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for batch deletion of storage files.
 * Handles bulk deletion on both object storage (S3, OVH) and database
 * with proper error handling for partial failures.
 */
@Slf4j
@Service
public class FileBatchDeletionService {

    private final StorageFileRepository storageFileRepository;
    private final List<FileStorageProviderService> fileStorageProviders;

    public FileBatchDeletionService(
            StorageFileRepository storageFileRepository,
            List<FileStorageProviderService> fileStorageProviders) {
        this.storageFileRepository = storageFileRepository;
        this.fileStorageProviders = fileStorageProviders;
    }

    /**
     * Result of a batch deletion operation.
     */
    public record BatchDeletionResult(
            int totalProcessed,
            int successfullyDeleted,
            int markedAsFailed,
            int skipped
    ) {
        public static BatchDeletionResult empty() {
            return new BatchDeletionResult(0, 0, 0, 0);
        }
    }

    /**
     * Delete a batch of storage files.
     * This method:
     * 1. Groups files by their storage provider(s)
     * 2. Performs bulk delete on each storage provider
     * 3. Tracks which files were successfully deleted from ALL their providers
     * 4. Bulk deletes successful files from database
     * 5. Marks failed files with DELETE_FAILED status
     *
     * @param storageFiles List of storage files to delete
     * @return BatchDeletionResult with statistics
     */
    @Transactional
    public BatchDeletionResult deleteBatch(List<StorageFile> storageFiles) {
        if (storageFiles == null || storageFiles.isEmpty()) {
            return BatchDeletionResult.empty();
        }

        log.info("Starting batch deletion of {} files", storageFiles.size());

        // Track deletion status for each file
        Set<Long> fullyDeletedFileIds = new HashSet<>();
        Set<Long> failedFileIds = new HashSet<>();
        Set<Long> skippedFileIds = new HashSet<>();

        // Track which providers each file still needs to be deleted from
        Map<Long, Set<String>> remainingProvidersPerFile = storageFiles.stream()
                .collect(Collectors.toMap(
                        StorageFile::getId,
                        f -> f.getProviders() != null ? new HashSet<>(f.getProviders()) : new HashSet<>()
                ));

        // Process files with no providers (just delete from DB)
        for (StorageFile file : storageFiles) {
            if (file.getProviders() == null || file.getProviders().isEmpty()) {
                fullyDeletedFileIds.add(file.getId());
            }
        }

        // Group files by provider for batch processing
        Map<ObjectStorageProvider, List<StorageFile>> filesByProvider = storageFiles.stream()
                .filter(f -> f.getProviders() != null && !f.getProviders().isEmpty())
                .flatMap(f -> f.getProviders().stream()
                        .map(p -> Map.entry(ObjectStorageProvider.valueOf(p), f)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        // Process each provider
        for (Map.Entry<ObjectStorageProvider, List<StorageFile>> entry : filesByProvider.entrySet()) {
            ObjectStorageProvider provider = entry.getKey();
            List<StorageFile> providerFiles = entry.getValue();

            log.info("Processing {} files for provider {}", providerFiles.size(), provider);

            BulkDeleteResult result = deleteFromProvider(provider, providerFiles);

            // Update tracking based on results
            for (StorageFile file : providerFiles) {
                String path = file.getPath();
                Set<String> remainingProviders = remainingProvidersPerFile.get(file.getId());

                if (result.successfulPaths().contains(path)) {
                    // Successfully deleted from this provider
                    remainingProviders.remove(provider.name());

                    // Check if file is now deleted from all providers
                    if (remainingProviders.isEmpty()) {
                        fullyDeletedFileIds.add(file.getId());
                    }
                } else if (result.failedPaths().containsKey(path)) {
                    // Failed to delete from this provider
                    String error = result.failedPaths().get(path);
                    log.warn("Failed to delete file {} from {}: {}", file.getId(), provider, error);
                    failedFileIds.add(file.getId());
                }
            }
        }

        // Remove failed files from fully deleted set (they may have succeeded on some providers)
        fullyDeletedFileIds.removeAll(failedFileIds);

        // Perform database operations
        int deletedFromDb = 0;
        int markedAsFailed = 0;

        if (!fullyDeletedFileIds.isEmpty()) {
            try {
                deletedFromDb = storageFileRepository.deleteAllByIdIn(fullyDeletedFileIds);
                log.info("Deleted {} records from database", deletedFromDb);
            } catch (Exception e) {
                log.error("Failed to delete records from database: {}", e.getMessage(), e);
                // Move these to failed since we couldn't delete from DB
                failedFileIds.addAll(fullyDeletedFileIds);
                fullyDeletedFileIds.clear();
            }
        }

        if (!failedFileIds.isEmpty()) {
            try {
                markedAsFailed = storageFileRepository.updateStatusByIdIn(failedFileIds, FileStorageStatus.DELETE_FAILED);
                log.info("Marked {} files as DELETE_FAILED", markedAsFailed);
            } catch (Exception e) {
                log.error("Failed to update status for failed files: {}", e.getMessage(), e);
            }
        }

        BatchDeletionResult result = new BatchDeletionResult(
                storageFiles.size(),
                deletedFromDb,
                markedAsFailed,
                skippedFileIds.size()
        );

        log.info("Batch deletion completed: {}", result);
        return result;
    }

    /**
     * Delete files from a specific provider using bulk delete API.
     */
    private BulkDeleteResult deleteFromProvider(ObjectStorageProvider provider, List<StorageFile> files) {
        try {
            FileStorageProviderService providerService = getStorageService(provider);

            // Extract paths
            List<String> paths = files.stream()
                    .map(StorageFile::getPath)
                    .toList();

            // Use appropriate bulk delete method based on provider
            if (provider == ObjectStorageProvider.S3) {
                // Group by bucket for S3
                Map<S3Bucket, List<StorageFile>> filesByBucket = files.stream()
                        .filter(f -> f.getBucket() != null)
                        .collect(Collectors.groupingBy(StorageFile::getBucket));

                Set<String> allSuccessful = new HashSet<>();
                Map<String, String> allFailed = new java.util.HashMap<>();

                for (Map.Entry<S3Bucket, List<StorageFile>> bucketEntry : filesByBucket.entrySet()) {
                    S3Bucket bucket = bucketEntry.getKey();
                    List<String> bucketPaths = bucketEntry.getValue().stream()
                            .map(StorageFile::getPath)
                            .toList();

                    BulkDeleteResult chunkResult = providerService.bulkDeleteV2(bucket, bucketPaths);
                    allSuccessful.addAll(chunkResult.successfulPaths());
                    allFailed.putAll(chunkResult.failedPaths());
                }

                return new BulkDeleteResult(allSuccessful, allFailed);

            } else {
                // OVH, Outscale - use legacy bulk delete
                // Process in chunks of MAX_OVH_BATCH_SIZE
                Set<String> allSuccessful = new HashSet<>();
                Map<String, String> allFailed = new java.util.HashMap<>();

                BulkDeleteResult chunkResult = providerService.bulkDelete(paths);
                allSuccessful.addAll(chunkResult.successfulPaths());
                allFailed.putAll(chunkResult.failedPaths());

                return new BulkDeleteResult(allSuccessful, allFailed);
            }
        } catch (Exception e) {
            log.error("Error during bulk delete for provider {}: {}", provider, e.getMessage(), e);
            List<String> paths = files.stream().map(StorageFile::getPath).toList();
            return BulkDeleteResult.allFailed(paths, e.getMessage());
        }
    }

    private FileStorageProviderService getStorageService(ObjectStorageProvider storageProvider) {
        return fileStorageProviders.stream()
                .filter(p -> p.getProvider() == storageProvider)
                .findFirst()
                .orElseThrow(() -> new ProviderNotFoundException("Provider not found: " + storageProvider));
    }
}
