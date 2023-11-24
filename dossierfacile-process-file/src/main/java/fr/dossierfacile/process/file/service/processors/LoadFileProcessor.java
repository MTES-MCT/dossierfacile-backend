package fr.dossierfacile.process.file.service.processors;

import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.service.AnalysisContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class LoadFileProcessor implements Processor {
    private FileStorageService fileStorageService;

    public AnalysisContext process(AnalysisContext analysisContext) {
        try {
            try (InputStream in = fileStorageService.download(analysisContext.getDfFile().getStorageFile())) {
                Path temporaryFile = Files.createTempFile("tax-" + analysisContext.getDfFile().getStorageFile().getId()
                                + "-" + UUID.randomUUID(),
                        MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(analysisContext.getDfFile().getStorageFile().getContentType()) ? ".pdf" : "");
                // actually we only need first file
                Files.copy(in, temporaryFile, StandardCopyOption.REPLACE_EXISTING);

                analysisContext.setFile(temporaryFile.toFile());
            }
        } catch (Exception e) {
            log.error("Cannot read and save files from document");
        }
        return analysisContext;
    }

    @Override
    public AnalysisContext cleanContext(AnalysisContext analysisContext) {
        if (analysisContext.getFile() != null) {
            try {
                analysisContext.getFile().delete();
            } catch (Exception e) {
                log.warn("Cannot clean up this file", e);
            }
            analysisContext.setFile(null);
        }
        return analysisContext;
    }
}
