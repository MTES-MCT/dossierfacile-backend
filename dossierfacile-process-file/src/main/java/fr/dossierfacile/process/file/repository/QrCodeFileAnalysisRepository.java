package fr.dossierfacile.process.file.repository;

import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCodeFileAnalysisRepository extends JpaRepository<QrCodeFileAnalysis, Long> {

    boolean existsByFileId(Long fileId);

}
