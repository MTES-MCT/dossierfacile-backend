package fr.dossierfacile.process.file.service.monfranceconnect.repository;

import fr.dossierfacile.common.entity.MonFranceConnectValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationResultRepository extends JpaRepository<MonFranceConnectValidationResult, Long> {

    boolean existsByFileId(Long fileId);

}
