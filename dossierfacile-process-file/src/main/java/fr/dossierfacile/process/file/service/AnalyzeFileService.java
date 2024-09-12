package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.processors.BarCodeFileProcessor;
import fr.dossierfacile.process.file.service.processors.FileParserProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class AnalyzeFileService {
    private final BarCodeFileProcessor barCodeFileProcessor;
    private final FileParserProcessor fileParserProcessor;
    private final FileRepository fileRepository;

    public void processFile(Long fileId) {
        Optional<File> optFile = fileRepository.findById(fileId);
        if (optFile.isPresent()) {
            barCodeFileProcessor.process(optFile.get());
            fileParserProcessor.process(optFile.get());
        }
    }
}
