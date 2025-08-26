package fr.dossierfacile.scheduler.tasks.garbagecollection;

import fr.dossierfacile.common.entity.TenantLastStatus;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.repository.TenantLogRepository;
import fr.dossierfacile.common.service.S3FileStorageServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class GarbageCollectionService {

    private static final int BATCH_SIZE = 100;

    private final TenantLogRepository tenantLogRepository;
    private final GarbageSequenceRepository garbageSequenceRepository;
    private final List<LogType> targetedLogStatus = List.of(LogType.ACCOUNT_DELETE, LogType.ACCOUNT_ARCHIVED, LogType.DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS);
    private final S3FileStorageServiceImpl s3Client;

    public void handleGarbageCollection() {
        var currentGarbageSequence = garbageSequenceRepository.getGarbageSequenceByName(GarbageSequenceName.TENANT_DOCUMENTS);
        if (currentGarbageSequence == null) {
            currentGarbageSequence = initGarbageSequence();
        }
        var batchToProcess = tenantLogRepository.findLastStatusBatch(currentGarbageSequence.value, BATCH_SIZE);
        try {
            batchToProcess.stream()
                    .filter(item -> targetedLogStatus.contains(item.getStatus()))
                    .forEach(this::ensureTenantDocumentDeleted);
            if (CollectionUtils.isNotEmpty(batchToProcess)) {
                currentGarbageSequence.setValue(currentGarbageSequence.getValue() + BATCH_SIZE);
                currentGarbageSequence.setLastUpdateDate(LocalDateTime.now());
                garbageSequenceRepository.save(currentGarbageSequence);
            } else {
                log.info("No more tenant to process");
            }
        } catch (Exception e) {
            log.error("Error while cleaning orphan files", e);
        }
    }

    private void ensureTenantDocumentDeleted(TenantLastStatus tenantLastStatus) {
        log.info("Ensuring documents deleted for tenantId: {}", tenantLastStatus.getTenantId());
        cleanFilesFromBucket(S3Bucket.RAW_FILE, tenantLastStatus.getTenantId());
        cleanFilesFromBucket(S3Bucket.RAW_MINIFIED, tenantLastStatus.getTenantId());
        cleanFilesFromBucket(S3Bucket.WATERMARK_DOC, tenantLastStatus.getTenantId());
        cleanFilesFromBucket(S3Bucket.FULL_PDF, tenantLastStatus.getTenantId());
    }

    private void cleanFilesFromBucket(S3Bucket bucket, Long tenantId) {
        log.info("Searching for left files inside bucket {} for tenantId: {}", bucket, tenantId);
        var prefix = String.format("tenant_%d/", tenantId);
        var orphanFiles = s3Client.listObjectNamesV2(bucket, prefix);
        if (CollectionUtils.isNotEmpty(orphanFiles)) {
            log.info("Found {} orphan files for tenant id : {} in bucket {}", orphanFiles.size(), tenantId, bucket);
            s3Client.deleteListOfObjects(bucket, orphanFiles);
        } else {
            log.info("No orphan files found for tenant id : {} in bucket {}", tenantId, bucket);
        }
    }

    private GarbageSequenceEntity initGarbageSequence() {
        var garbageSequence = GarbageSequenceEntity.builder()
                .name(GarbageSequenceName.TENANT_DOCUMENTS)
                .value(0)
                .lastUpdateDate(LocalDateTime.now())
                .build();
        return garbageSequenceRepository.save(garbageSequence);
    }
}
