package fr.dossierfacile.process.file.repository;

import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarCodeFileAnalysisRepository extends JpaRepository<BarCodeFileAnalysis, Long> {

    boolean existsByFileId(Long fileId);

    default boolean hasNotAlreadyBeenAnalyzed(File file) {
        boolean resultExists = existsByFileId(file.getId());
        return !resultExists;
    }

}
