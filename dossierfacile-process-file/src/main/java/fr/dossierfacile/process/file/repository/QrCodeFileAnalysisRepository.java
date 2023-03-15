package fr.dossierfacile.process.file.repository;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCodeFileAnalysisRepository extends JpaRepository<QrCodeFileAnalysis, Long> {

    boolean existsByFileId(Long fileId);

    default boolean hasNotAlreadyBeenAnalyzed(File file) {
        boolean resultExists = existsByFileId(file.getId());
        return !resultExists;
    }

}
