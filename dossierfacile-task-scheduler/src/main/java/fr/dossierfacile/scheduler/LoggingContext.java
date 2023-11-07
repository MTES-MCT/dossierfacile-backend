package fr.dossierfacile.scheduler;

import fr.dossierfacile.common.entity.StorageFile;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class LoggingContext {

    private static final String STORAGE_FILE = "storage_file_id";

    public static void setStorageFile(StorageFile file) {
        if (file.getId() != null) {
            MDC.put(STORAGE_FILE, file.getId().toString());
        }
    }

    public static void clear() {
        MDC.remove(STORAGE_FILE);
    }

}
