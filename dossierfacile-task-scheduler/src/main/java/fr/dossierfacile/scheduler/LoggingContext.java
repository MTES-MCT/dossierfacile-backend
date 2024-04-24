package fr.dossierfacile.scheduler;

import fr.dossierfacile.scheduler.tasks.TaskName;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class LoggingContext {

    public static final String STORAGE_FILE = "storage_file_id";
    private static final String TASK_NAME = "task";

    public static void startTask(TaskName taskName) {
        MDC.put(TASK_NAME, taskName.name());
        log.debug("Starting scheduled task");
    }

    public static void endTask() {
        log.debug("Finished scheduled task");
        MDC.clear();
    }

    public static void put(String key, Object value) {
        MDC.put(key, String.valueOf(value));
    }

    public static void remove(String key) {
        MDC.remove(key);
    }

    public static void clear() {
        MDC.remove(TASK_NAME);
        MDC.remove(STORAGE_FILE);
    }

}
