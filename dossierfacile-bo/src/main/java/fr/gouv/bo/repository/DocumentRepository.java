package fr.gouv.bo.repository;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query(value = """
            SELECT d.id
            FROM Document d
            WHERE (d.last_modified_date IS NULL OR d.last_modified_date < :to)
              AND d.watermark_file_id IS NULL
            """, nativeQuery = true)
    List<Long> findWithoutPDFToDate(@Param("to") LocalDateTime toDateTime);

    @Modifying
    @Query("UPDATE Document d SET d.documentDeniedReasons = :documentDeniedReasons where d.id = :documentId")
    void updateDocumentWithDocumentDeniedReasons(@Param("documentDeniedReasons") DocumentDeniedReasons documentDeniedReasons, @Param("documentId") Long documentId);

    Optional<Document> findByName(String name);
}
