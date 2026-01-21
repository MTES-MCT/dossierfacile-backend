package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentIAFileAnalysisRepository extends JpaRepository<DocumentIAFileAnalysis, Long> {

    Optional<DocumentIAFileAnalysis> findByDocumentIaExecutionId(String id);

}