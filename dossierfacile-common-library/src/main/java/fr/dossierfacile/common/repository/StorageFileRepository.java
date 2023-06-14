package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.StorageFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {
    List<StorageFile> findAllByName(String s);
}