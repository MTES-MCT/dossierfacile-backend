package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query(value = "SELECT d.* FROM document d WHERE d.creation_date is null ORDER BY d.id DESC", nativeQuery = true)
    Page<Document> findDocumentsByCreationDateTimeIsNull(Pageable pageable);

    @Query("SELECT d.id FROM Document d " +
            "WHERE d.name is null " +
            "and d.processingStartTime is not null " +
            "and d.processingEndTime is null " +
            "and d.processingStartTime < :timeAgo " +
            "ORDER BY d.id")
    Page<Long> findAllFailedGeneratedPdfDocumentIdsSinceXTimeAgo(@Param("timeAgo") LocalDateTime timeAgo, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE document SET retries = 0, locked = false, locked_by = null where id in (" +
            "SELECT d.id FROM document d WHERE d.name is null and d.processing_start_time is not null and d.processing_end_time is null and d.processing_start_time < now() - (interval '12' hour)" +
            ")", nativeQuery = true)
    void unlockFailedPdfDocumentsGeneratedUsingButtonRequest();

    @Modifying
    @Query(value = "UPDATE document SET retries = 0, locked = false, locked_by = null where id in (" +
            "SELECT d.id FROM document d WHERE d.name is null and d.processing_start_time is not null and d.processing_end_time is null and d.processing_start_time < now() - (interval '24' hour)" +
            ")", nativeQuery = true)
    void unlockFailedPdfDocumentsGeneratedUsingScheduledTask();

    @Modifying
    @Query("UPDATE Document d SET d.documentDeniedReasons = :documentDeniedReasons where d.id = :documentId")
    void updateDocumentWithDocumentDeniedReasons(@Param("documentDeniedReasons") DocumentDeniedReasons documentDeniedReasons, @Param("documentId") Long documentId);
}
