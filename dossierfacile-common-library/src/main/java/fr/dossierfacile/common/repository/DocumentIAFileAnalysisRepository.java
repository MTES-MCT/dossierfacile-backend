package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentIAFileAnalysisRepository extends JpaRepository<DocumentIAFileAnalysis, Long> {

    Optional<DocumentIAFileAnalysis> findByDocumentIaExecutionId(String id);

    List<DocumentIAFileAnalysis> findAllByAnalysisStatus(DocumentIAFileAnalysisStatus analysisStatus, Pageable pageable);

}