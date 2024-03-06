package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParsedFileAnalysisRepository extends JpaRepository<ParsedFileAnalysis, Long> {
    List<ParsedFileAnalysis> findByFileId(Long fileId);
}
