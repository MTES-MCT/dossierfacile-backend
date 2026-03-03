package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.repository.FileRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.MinifyFileService;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.ProcessFileService;
import fr.dossierfacile.api.pdfgenerator.service.processor.MetadataFileProcessor;
import fr.dossierfacile.common.service.FileStorageServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@AllArgsConstructor
@Service
@Slf4j
public class ProcessFileServiceImpl implements ProcessFileService {

    private MinifyFileService minifyFileService;
    private FileRepository fileRepository;
    private FileStorageServiceImpl fileStorageService;
    private MetadataFileProcessor metadataFileProcessor;

    @Override
    @Transactional
    public void process(Long fileId) {

        var file = fileRepository.findById(fileId);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File not found");
        }

        var safeFile = file.get();
        Path tempFile = null;
        try (InputStream inputStream = fileStorageService.download(safeFile.getStorageFile())) {
            tempFile = Files.createTempFile("df-file-", ".bin");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // We have to create 2 streams because when an input stream is read, the buffer is empty
            try (
                    InputStream minifyStream = Files.newInputStream(tempFile);
                    InputStream metadataStream = Files.newInputStream(tempFile)
            ) {
                try {
                    minifyFileService.process(minifyStream, safeFile);
                } catch (Exception exception) {
                    log.error("Minify processing failed for fileId={}", fileId, exception);
                }

                try {
                    metadataFileProcessor.process(metadataStream, safeFile);
                } catch (Exception exception) {
                    log.error("Metadata processing failed for fileId={}", fileId, exception);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to download file", e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception exception) {
                    // Best-effort cleanup for temp file.
                    // The file will be deleted automatically when scalingo container restarts
                    log.error("Unable to delete temp file", exception);
                }
            }
        }
    }

}
