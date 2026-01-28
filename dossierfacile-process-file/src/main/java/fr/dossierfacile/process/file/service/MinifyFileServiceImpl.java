package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.interfaces.MinifyFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinifyFileServiceImpl implements MinifyFileService {

    private final FileRepository fileRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;
    private final DocumentHelperService documentHelperService;

    @Override
    public void process(Long fileId) {
        fileRepository.findById(fileId)
                .ifPresent(file -> {
                    StorageFile newPreview = null;
                    try (InputStream inputStream = fileStorageService.download(file.getStorageFile())) {
                        newPreview = documentHelperService.generatePreview(file.getDocument(), inputStream, file.getStorageFile().getName());

                        // This is a long operation and method is not transactional - refresh to check status
                        Optional<File> dbFile = fileRepository.findById(file.getId());
                        if (dbFile.isPresent()) {
                            // Mark previous preview as TO_DELETE before replacing
                            StorageFile previousPreview = dbFile.get().getPreview();
                            if (previousPreview != null) {
                                log.info("Replacing previous preview {} for fileId={}", previousPreview.getId(), fileId);
                                fileStorageService.delete(previousPreview);
                            }

                            dbFile.get().setPreview(newPreview);
                            fileRepository.save(dbFile.get());
                        } else {
                            log.warn("File {} was deleted during preview generation, marking preview as TO_DELETE", fileId);
                            newPreview.setStatus(FileStorageStatus.TO_DELETE);
                            storageFileRepository.save(newPreview);
                        }

                    } catch (Exception e) {
                        log.error("Error during preview generation for fileId={}: {}", fileId, e.getMessage(), e);
                        // Mark the new preview as TO_DELETE if it was created but not associated
                        if (newPreview != null && newPreview.getId() != null) {
                            newPreview.setStatus(FileStorageStatus.TO_DELETE);
                            storageFileRepository.save(newPreview);
                        }
                    }
                });
    }

}
