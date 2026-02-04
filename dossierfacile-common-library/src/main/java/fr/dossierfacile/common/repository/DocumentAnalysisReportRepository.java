package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentAnalysisReportRepository extends JpaRepository<DocumentAnalysisReport, Long> {

    Optional<DocumentAnalysisReport> findByDocumentId(Long documentId);

}
