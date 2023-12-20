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
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query(value = "SELECT d.* FROM document d WHERE d.creation_date is null ORDER BY d.id DESC", nativeQuery = true)
    Page<Document> findDocumentsByCreationDateTimeIsNull(Pageable pageable);

    @Query(value = """
            SELECT d.id
            FROM Document d
            WHERE d.document_status = 'TO_PROCESS'
              AND (d.last_modified_date IS NULL OR d.last_modified_date < :to)
              AND d.watermark_file_id IS NULL
            """, nativeQuery = true)
    List<Long> findToProcessWithoutPDFToDate(@Param("to") LocalDateTime toDateTime);

    @Modifying
    @Query("UPDATE Document d SET d.documentDeniedReasons = :documentDeniedReasons where d.id = :documentId")
    void updateDocumentWithDocumentDeniedReasons(@Param("documentDeniedReasons") DocumentDeniedReasons documentDeniedReasons, @Param("documentId") Long documentId);

    Optional<Document> findByName(String name);
}
