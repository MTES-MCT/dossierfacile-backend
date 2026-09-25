package fr.dossierfacile.common.enums;

/**
 * Origin of an automatic document deletion (neither the tenant nor an operator is involved).
 * Stored in DocumentLogDetails.source of the DOCUMENT_DELETED log; absent for user and operator deletions.
 */
public enum DocumentDeletionSource {
    /** task-scheduler: the watermark PDF generation kept failing for longer than the configured delay. */
    FAILED_PDF_CLEANUP
}
