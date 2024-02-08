package fr.dossierfacile.process.file.service;

import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.processors.BarCodeFileProcessor;
import fr.dossierfacile.process.file.service.processors.FileParserProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AnalyzeFile {
    private final BarCodeFileProcessor barCodeFileProcessor;
    private final FileParserProcessor fileParserProcessor;
    private final FileRepository fileRepository;

    public void processFile(Long fileId) {
        fileRepository.findById(fileId)
                .map(barCodeFileProcessor::process)
                .map(fileParserProcessor::process);
    }
}
