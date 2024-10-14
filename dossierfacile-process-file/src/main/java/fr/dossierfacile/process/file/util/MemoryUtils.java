package fr.dossierfacile.process.file.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryUtils {
    // TODO currently we only log low availability
    public static boolean hasEnoughAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        // Arbitrary choose 250MB as minimal requirement to perform
        if (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory() < 419430400) { // (419430400  = 400 MB)
            log.warn("Memory usage: (Total=" + runtime.totalMemory() / 1048576 + " MB , max=" + runtime.maxMemory() / 1048576 + " MB , free=" + runtime.freeMemory() / 1048576 + " MB , avail=" + ((runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory())/1048576));
        }
        return true;
    }
    public static void logMemory(){
        Runtime runtime = Runtime.getRuntime();
        log.warn("Memory usage: (Total=" + runtime.totalMemory() / 1048576 + " MB , max=" + runtime.maxMemory() / 1048576 + " MB , free=" + runtime.freeMemory() / 1048576 + " MB , avail=" + ((runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory())/1048576));
    }
}
