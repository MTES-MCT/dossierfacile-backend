package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileCommonRepository extends JpaRepository<File, Long> {
}