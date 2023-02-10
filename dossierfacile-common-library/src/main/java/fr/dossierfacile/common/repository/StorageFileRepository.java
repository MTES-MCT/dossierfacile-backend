package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.StorageFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {
}