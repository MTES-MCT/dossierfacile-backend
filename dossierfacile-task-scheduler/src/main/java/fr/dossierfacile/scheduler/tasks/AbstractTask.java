package fr.dossierfacile.scheduler.tasks;


import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.logging.util.LoggerUtil;
import fr.dossierfacile.logging.task.LogAggregator;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class AbstractTask {

    @Lookup
    protected LogAggregator logAggregator() {
        return null; // Spring will implement this @Lookup method
    }

    protected void startTask(TaskName taskName) {
        LoggerUtil.prepareMDCForScheduledTask(taskName.name());
    }

    protected void endTask() {
        logAggregator().sendLogs();
    }

    protected void addTenantIdsToDeleteForLogging(List<Long> tenantIds) {
        if (!CollectionUtils.isEmpty(tenantIds)) {
            var ids = tenantIds.stream().map(String::valueOf).collect(Collectors.joining(" , "));
            LoggerUtil.addTaskTenantList("[" + ids + "]");
        }
    }

    protected void addFileIdListForLogging(List<StorageFile> files) {
        if (!CollectionUtils.isEmpty(files)) {
            var ids = files.stream().map(StorageFile::getId).map(String::valueOf).collect(Collectors.joining(" , "));
            LoggerUtil.addTaskStorageFileList("[" + ids + "]");
        }
    }

    protected void addDocumentIdListForLogging(List<Document> documents) {
        if (!CollectionUtils.isEmpty(documents)) {
            var ids = documents.stream().map(Document::getId).map(String::valueOf).collect(Collectors.joining(" , "));
            LoggerUtil.addTaskDocumentList("[" + ids + "]");
        }
    }

}
