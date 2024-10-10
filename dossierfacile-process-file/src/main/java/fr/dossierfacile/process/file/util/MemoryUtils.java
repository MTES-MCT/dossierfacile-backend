package fr.dossierfacile.process.file.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryUtils {
    // TODO currently we only log low availability
    public static boolean hasEnoughAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        // Arbitrary choose 250MB as minimal requirement to perform
        if (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory() < 262144000) {
            log.warn("Memory usage: (Total=" + runtime.totalMemory() / 1024 + " MB , max=" + runtime.maxMemory() / 1024 + " MB , free=" + runtime.freeMemory() / 1024 + " MB , avail=" + (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()));
        }
        return true;
    }
}
