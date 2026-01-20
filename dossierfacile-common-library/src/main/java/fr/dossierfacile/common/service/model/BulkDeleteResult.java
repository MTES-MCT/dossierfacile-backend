package fr.dossierfacile.common.service.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Result of a bulk delete operation on object storage.
 * Tracks which paths were successfully deleted and which failed.
 */
public record BulkDeleteResult(
        Set<String> successfulPaths,
        Map<String, String> failedPaths // path -> error message
) {
    public boolean hasFailures() {
        return !failedPaths.isEmpty();
    }

    public boolean isCompleteSuccess() {
        return failedPaths.isEmpty();
    }

    public int totalProcessed() {
        return successfulPaths.size() + failedPaths.size();
    }

    public static BulkDeleteResult allSuccessful(List<String> paths) {
        return new BulkDeleteResult(Set.copyOf(paths), Map.of());
    }

    public static BulkDeleteResult allFailed(List<String> paths, String errorMessage) {
        Map<String, String> failedPaths = new HashMap<>();
        for (String path : paths) {
            failedPaths.put(path, errorMessage);
        }
        return new BulkDeleteResult(Set.of(), failedPaths);
    }

    public static BulkDeleteResult empty() {
        return new BulkDeleteResult(Set.of(), Map.of());
    }
}
