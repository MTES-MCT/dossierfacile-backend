package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.File;

import java.util.Optional;

public interface SharedFileService {
    Optional<File> findByPath(String filename);

    Optional<File> findById(Long id);
}