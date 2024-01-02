package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class StorageFileLoaderService {
    private FileStorageService fileStorageService;

    public File getTemporaryFilePath(StorageFile storageFile) {
        try {
            try (InputStream in = fileStorageService.download(storageFile)) {
                Path temporaryFile = Files.createTempFile("tax-" + storageFile.getId()
                                + "-" + UUID.randomUUID(),
                        MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(storageFile.getContentType()) ? ".pdf" : "");
                // actually we only need first file
                if (Files.copy(in, temporaryFile, StandardCopyOption.REPLACE_EXISTING) > 0) {
                    return temporaryFile.toFile();
                }
            }
        } catch (Exception e) {
            log.error("Cannot read and save files from document");
        }
        return null;
    }

    public void removeFileIfExist(File file) {
        if (file != null) {
            try {
                file.delete();
            } catch (Exception e) {
                log.warn("Cannot clean up this file", e);
            }
        }
    }
}
