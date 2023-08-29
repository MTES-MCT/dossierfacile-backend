package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.FileAnalysisCriteria;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.qrcodeanalysis.BarCodeFileProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AnalyzeFile {

    private final BarCodeFileProcessor barCodeFileProcessor;
    private final FileRepository fileRepository;

    public void processFile(Long fileId) {
        fileRepository.findById(fileId)
                .filter(AnalyzeFile::shouldBeAnalyzed)
                .ifPresent(barCodeFileProcessor::process);
    }

    private static boolean shouldBeAnalyzed(File file) {
        if (FileAnalysisCriteria.shouldBeAnalyzed(file)) {
            return true;
        }
        log.info("File is not eligible for analysis");
        return false;
    }

}
