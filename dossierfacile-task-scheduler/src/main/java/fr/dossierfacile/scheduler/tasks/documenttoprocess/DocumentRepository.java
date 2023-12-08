package fr.dossierfacile.scheduler.tasks.documenttoprocess;

import fr.dossierfacile.common.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query(value = """
            SELECT *
            FROM document
            WHERE document_status = 'TO_PROCESS'
              AND (last_modified_date IS NULL OR last_modified_date < :to)
              AND watermark_file_id IS NULL
              LIMIT 100;
            """, nativeQuery = true)
    List<Document> findToProcessWithoutPDFToDate(LocalDateTime toDateTime);
}
