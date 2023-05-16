package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.repository.SharedFileRepository;
import fr.dossierfacile.common.service.interfaces.SharedFileService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class SharedFileServiceImpl implements SharedFileService {
    private final SharedFileRepository repository;

    @Override
    public Optional<File> findById(Long id) {
        return repository.findById(id);
    }
}