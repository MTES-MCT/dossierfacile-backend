package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParsedFileAnalysisRepository extends JpaRepository<ParsedFileAnalysis, Long> {
}
