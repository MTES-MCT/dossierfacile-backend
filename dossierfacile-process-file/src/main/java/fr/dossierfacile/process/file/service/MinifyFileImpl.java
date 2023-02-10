package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.interfaces.MinifyFile;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinifyFileImpl implements MinifyFile {

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final DocumentHelperService documentHelperService;

    @Override
    public void process(Long fileId) {
        fileRepository.findById(fileId)
                .ifPresent(file -> {
                    try (InputStream inputStream = fileStorageService.download(file)) {
                        StorageFile storageFile = documentHelperService.generatePreview(inputStream, file.getOriginalName());

                        file.setPreview(storageFile);
                        fileRepository.save(file);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e.getCause());
                        Sentry.captureException(e);
                    }
                });
    }

}
