package fr.dossierfacile.scheduler;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class LoggingContext {

    private static final String STORAGE_FILE = "storage_file_id";
    private static final String TASK_NAME = "task";

    public static void startTask(TaskName taskName) {
        MDC.put(TASK_NAME, taskName.name());
        log.debug("Starting scheduled task");
    }

    public static void endTask() {
        log.debug("Finished scheduled task");
        MDC.clear();
    }

    public static void setStorageFile(StorageFile file) {
        if (file.getId() != null) {
            MDC.put(STORAGE_FILE, file.getId().toString());
        }
    }

    public static void clear() {
        MDC.remove(TASK_NAME);
        MDC.remove(STORAGE_FILE);
    }

}
