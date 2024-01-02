package fr.dossierfacile.process.file.service;

import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.processors.BarCodeFileProcessor;
import fr.dossierfacile.process.file.service.processors.OcrParserFileProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class AnalyzeFile {
    private final BarCodeFileProcessor barCodeFileProcessor;
    private final OcrParserFileProcessor ocrParserFileProcessor;
    private final FileRepository fileRepository;

    public void processFile(Long fileId) {
        Optional.ofNullable(fileRepository.findById(fileId).orElse(null))
                .filter(Objects::nonNull)
                .map(barCodeFileProcessor::process)
                .map(ocrParserFileProcessor::process);
    }
}
