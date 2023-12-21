package fr.dossierfacile.scheduler.tasks.document;

import fr.dossierfacile.common.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query(value = """
            SELECT *
            FROM document
            WHERE watermark_file_id IS NULL
              AND (last_modified_date IS NULL OR last_modified_date < :to)
            ORDER BY
              CASE WHEN last_modified_date IS NULL THEN 1 ELSE 0 END,
              last_modified_date DESC
            LIMIT 200;
            """, nativeQuery = true)
    List<Document> findWithoutPDFToDate(@Param("to") LocalDateTime toDateTime);

    @Query("""
            SELECT d
            FROM Document d
            LEFT JOIN FETCH d.guarantor g
            WHERE d.watermarkFile IS NULL 
            AND ((d.lastModifiedDate IS NOT NULL AND d.lastModifiedDate < :to) 
            OR (d.lastModifiedDate IS NULL AND d.creationDateTime < :to))
            ORDER BY d.lastModifiedDate DESC
            """)
    List<Document> findDocumentWithoutPDFToDate(@Param("to") LocalDateTime toDateTime);

}
