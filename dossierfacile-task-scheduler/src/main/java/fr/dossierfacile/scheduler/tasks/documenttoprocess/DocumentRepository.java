package fr.dossierfacile.scheduler.tasks.documenttoprocess;

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
            WHERE document_status = 'TO_PROCESS'
              AND watermark_file_id IS NULL
              AND (last_modified_date IS NULL OR last_modified_date < :to)
            ORDER BY
              CASE WHEN last_modified_date IS NULL THEN 1 ELSE 0 END,
              last_modified_date DESC
            LIMIT 200;
            """, nativeQuery = true)
    List<Document> findToProcessWithoutPDFToDate(@Param("to") LocalDateTime toDateTime);
}
