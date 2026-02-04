package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentIAFileAnalysisRepository extends JpaRepository<DocumentIAFileAnalysis, Long> {

    Optional<DocumentIAFileAnalysis> findByDocumentIaExecutionId(String id);

    List<DocumentIAFileAnalysis> findAllByAnalysisStatus(DocumentIAFileAnalysisStatus analysisStatus, Pageable pageable);

    @Query("SELECT COUNT(difa) FROM DocumentIAFileAnalysis difa " +
           "WHERE difa.file.document.id = :documentId " +
           "AND difa.analysisStatus IN ('SUCCESS', 'FAILED')")
    Long countAnalyzedFilesByDocumentId(@Param("documentId") Long documentId);

    @Query("SELECT COUNT(difa) FROM DocumentIAFileAnalysis difa " +
           "WHERE difa.file.document.id = :documentId")
    Long countTotalFilesByDocumentId(@Param("documentId") Long documentId);

}