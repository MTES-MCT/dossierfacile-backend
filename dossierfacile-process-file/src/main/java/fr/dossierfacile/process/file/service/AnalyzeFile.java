package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.FileAnalysisCriteria;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.qrcodeanalysis.BarCodeFileProcessor;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnalyzeFile {

    private final BarCodeFileProcessor barCodeFileProcessor;
    private final FileRepository fileRepository;

    public void processFile(Long fileId) {
        fileRepository.findById(fileId)
                .filter(FileAnalysisCriteria::shouldBeAnalyzed)
                .ifPresent(barCodeFileProcessor::process);
    }

}
