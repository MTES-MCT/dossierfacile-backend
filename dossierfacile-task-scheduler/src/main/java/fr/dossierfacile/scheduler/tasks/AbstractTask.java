package fr.dossierfacile.scheduler.tasks;


import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.logging.util.LoggerUtil;
import fr.dossierfacile.logging.task.LogAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
public class AbstractTask {

    @Lookup
    protected LogAggregator logAggregator() {
        return null; // Spring will implement this @Lookup method
    }

    protected void startTask(TaskName taskName) {
        LoggerUtil.prepareMDCForScheduledTask(taskName.name());
        log.info("Starting task {}", taskName);
    }

    protected void endTask() {
        log.info("Ending task");
        logAggregator().sendLogs();
    }

    protected void countTenantIdForLogging(List<Long> tenantIds) {
        if (!CollectionUtils.isEmpty(tenantIds)) {
            LoggerUtil.addTaskTenantCount(tenantIds.size());
        }
    }

    protected void countFileIdForLogging(List<StorageFile> files) {
        if (!CollectionUtils.isEmpty(files)) {
            LoggerUtil.addTaskStorageFileCount(files.size());
        }
    }

    protected void countDocumentIdForLogging(List<Document> documents) {
        if (!CollectionUtils.isEmpty(documents)) {
            LoggerUtil.addTaskDocumentCount(documents.size());
        }
    }

}
