package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.processors.BarCodeFileProcessor;
import fr.dossierfacile.process.file.service.processors.LoadFileProcessor;
import fr.dossierfacile.process.file.service.processors.OcrParserFileProcessor;
import fr.dossierfacile.process.file.util.FileParsingEligibilityCriteria;
import fr.dossierfacile.process.file.util.QrCodeFileAnalysisCriteria;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;

@Slf4j
@Service
@AllArgsConstructor
public class AnalyzeFile {

    private final LoadFileProcessor loadFileProcessor;
    private final BarCodeFileProcessor barCodeFileProcessor;
    private final OcrParserFileProcessor ocrParserFileProcessor;
    private final FileRepository fileRepository;

    public void processFile(Long fileId) {
        AnalysisContext context = new AnalysisContext();

        Optional.of(context)
                .map(ctx -> {
                    ctx.setDfFile(fileRepository.findById(fileId).orElse(null));
                    return ctx;
                })
                .filter(ctx -> QrCodeFileAnalysisCriteria.shouldBeAnalyzed(ctx.getDfFile()))
                .map(barCodeFileProcessor::process);

        Optional.of(context)
                .filter(ctx -> FileParsingEligibilityCriteria.shouldBeParse(ctx.getDfFile()))
                .map(loadFileProcessor::process)
                .map(ocrParserFileProcessor::process)
                .map(loadFileProcessor::cleanContext);
    }



}
