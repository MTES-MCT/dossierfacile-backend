package fr.dossierfacile.common.repository;

import fr.dossierfacile.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedFileRepository extends JpaRepository<File, Long> {
    Optional<File> findByPath(String path);
}