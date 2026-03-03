package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.repository.FileRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.MinifyFileService;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinifyFileServiceImpl implements MinifyFileService {

    private final FileRepository fileRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileStorageService fileStorageService;
    private final DocumentHelperService documentHelperService;

    @Override
    @Transactional
    public void process(InputStream inputStream, File file) {

        StorageFile newPreview = null;
        try {
            newPreview = documentHelperService.generatePreview(file.getDocument(), inputStream, file.getStorageFile().getName());

            var previousPreview = file.getPreview();
            if (previousPreview != null) {
                log.info("Replacing previous preview {} for fileId={}", previousPreview.getId(), file.getId());
                fileStorageService.delete(previousPreview);
            }
            file.setPreview(newPreview);
            fileRepository.saveAndFlush(file);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure while saving preview for fileId={}: {}", file.getId(), e.getMessage());
            handleNewPreviewDeletion(newPreview);
        } catch (Exception e) {
            log.error("Error during preview generation for fileId={}: {}", file.getId(), e.getMessage(), e);
            handleNewPreviewDeletion(newPreview);
        }
    }

    private void handleNewPreviewDeletion(StorageFile newPreview) {
        if (newPreview != null && newPreview.getId() != null) {
            newPreview.setStatus(FileStorageStatus.TO_DELETE);
            fileStorageService.hardDelete(newPreview);
            storageFileRepository.save(newPreview);
        }
    }

}
