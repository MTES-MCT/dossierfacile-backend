package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.FileAnalysisCriteria;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.processors.LoadFileProcessor;
import fr.dossierfacile.process.file.service.processors.OcrParserFileProcessor;
import fr.dossierfacile.process.file.service.processors.BarCodeFileProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class AnalyzeFile {

    private final LoadFileProcessor loadFileProcessor;
    private final BarCodeFileProcessor barCodeFileProcessor;
    private final OcrParserFileProcessor ocrParserFileProcessor;
    private final FileRepository fileRepository;

    private static boolean shouldBeAnalyzed(File file) {
        if (FileAnalysisCriteria.shouldBeAnalyzed(file)) {
            return true;
        }
        log.info("File is not eligible for analysis");
        return false;
    }

    public void processFile(Long fileId) {
        AnalysisContext context = new AnalysisContext();

        Optional.of(context)
                .map(ctx -> {
                    ctx.setDfFile(fileRepository.findById(fileId).orElse(null));
                    return ctx;
                })
                .filter(ctx -> AnalyzeFile.shouldBeAnalyzed(ctx.getDfFile()))
                .map(loadFileProcessor::process)
                .map(barCodeFileProcessor::process)
                .map(ocrParserFileProcessor::process)
                .map(loadFileProcessor::cleanContext);
    }

}
