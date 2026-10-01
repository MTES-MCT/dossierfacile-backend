package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;

import java.util.List;

/** Outcome of the cleanup of one tenant; the deleted documents are detached entities kept for notification purposes only. */
public record FailedPdfCleanupResult(Long tenantId, List<Document> deletedDocuments) {

    public boolean nothingDeleted() {
        return deletedDocuments.isEmpty();
    }
}
