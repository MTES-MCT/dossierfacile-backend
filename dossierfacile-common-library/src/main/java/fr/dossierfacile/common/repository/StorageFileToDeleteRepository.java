package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.StorageFileToDelete;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageFileToDeleteRepository extends JpaRepository<StorageFileToDelete, Long> {
}