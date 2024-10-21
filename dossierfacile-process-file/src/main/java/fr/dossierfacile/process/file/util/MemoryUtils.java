package fr.dossierfacile.process.file.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryUtils {
    public static final int INT1024X1024 = 1048576;

    public static void logAvailableMemory(int thresholdInMB) {
        Runtime runtime = Runtime.getRuntime();
        if (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory() < (long) thresholdInMB * INT1024X1024) {
            log.warn("Memory usage: (Total=" + runtime.totalMemory() / INT1024X1024 + " MB , max=" + runtime.maxMemory() / INT1024X1024 + " MB , free=" + runtime.freeMemory() / INT1024X1024 + " MB , avail=" + ((runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / INT1024X1024));
        }
    }
}
