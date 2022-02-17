package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentPdfGenerationLogRepository extends JpaRepository<DocumentPdfGenerationLog, Long> {
    @Modifying
    @Query("UPDATE DocumentPdfGenerationLog d SET d.deactivated = true where d.documentId = :documentId and d.id > :logId")
    void deactivateNewerMessages(@Param("documentId") Long documentId, @Param("logId") Long logId);

    @Modifying
    @Query("UPDATE DocumentPdfGenerationLog d SET d.deactivated = true where d.id = :id")
    void updateDocumentPdfGenerationLog(@Param("id") Long id);
}
