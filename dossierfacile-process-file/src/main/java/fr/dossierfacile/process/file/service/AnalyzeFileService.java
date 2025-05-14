package fr.dossierfacile.process.file.service;

import co.elastic.apm.api.CaptureTransaction;
import co.elastic.apm.api.ElasticApm;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.processors.BarCodeFileProcessor;
import fr.dossierfacile.process.file.service.processors.FileParserProcessor;
import fr.dossierfacile.process.file.service.processors.blurry.BlurryProcessor;
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
    private final BlurryProcessor blurryProcessor;
    private final FileRepository fileRepository;

    @CaptureTransaction(type="ANALYSIS", value = "FileAnalysis")
    public void processFile(Long fileId) {
        Optional<File> optFile = fileRepository.findById(fileId);
        if (optFile.isPresent()) {
            barCodeFileProcessor.process(optFile.get());
            fileParserProcessor.process(optFile.get());
            blurryProcessor.process(optFile.get());
        }
    }
}
