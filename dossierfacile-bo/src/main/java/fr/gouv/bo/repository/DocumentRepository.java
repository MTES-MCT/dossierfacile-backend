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
            WHERE d.documentStatus = 'TO_PROCESS'
              AND (d.lastModifiedDate IS NULL OR d.lastModifiedDate < :toDateTime)
              AND d.watermarkFile IS NULL
            """)
    Page<Long> findToProcessWithoutPDFToDate(LocalDateTime toDateTime, Pageable pageable);

    @Modifying
    @Query("UPDATE Document d SET d.documentDeniedReasons = :documentDeniedReasons where d.id = :documentId")
    void updateDocumentWithDocumentDeniedReasons(@Param("documentDeniedReasons") DocumentDeniedReasons documentDeniedReasons, @Param("documentId") Long documentId);

    Optional<Document> findByName(String name);
}
