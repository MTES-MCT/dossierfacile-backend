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
              AND last_modified_date > :from
              AND last_modified_date < :to
              AND NOT EXISTS (
                SELECT 1 FROM document_analysis_report
                WHERE document_id = document.id
            )
            """, nativeQuery = true)
    List<Document> findToProcessWithoutAnalysisReportBetweenDate(@Param("from") LocalDateTime fromDateTime, @Param("to") LocalDateTime toDateTime);

}
