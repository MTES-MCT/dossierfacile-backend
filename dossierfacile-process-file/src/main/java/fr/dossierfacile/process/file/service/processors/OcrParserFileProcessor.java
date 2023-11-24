package fr.dossierfacile.process.file.service.processors;

import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.common.repository.ParsedFileAnalysisRepository;
import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.AnalysisContext;
import fr.dossierfacile.process.file.service.ocr.TaxIncomeLeafParser;
import fr.dossierfacile.process.file.service.ocr.TaxIncomeParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class OcrParserFileProcessor implements Processor {
    private final FileRepository fileRepository;
    private final ParsedFileAnalysisRepository parsedFileAnalysisRepository;
    private final TaxIncomeParser taxIncomeParser;
    private final TaxIncomeLeafParser taxIncomeLeafParser;

    public AnalysisContext process(AnalysisContext context) {
        if (context.getDfFile().getDocument().getDocumentCategory() != DocumentCategory.TAX) {
            log.info("This file type not elligible to this process");
            return null;
        }
        if (context.getFile() == null || !context.getFile().exists()) {
            log.error("File cannot be empty dfFile=" + context.getDfFile().getId());
            return null;
        }
        // Ideally we should do the classification operation before parsing
        try {
            TaxIncomeMainFile parsedDocument = taxIncomeParser.parse(context.getFile());
            ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder()
                    .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                    .parsedFile(parsedDocument)
                    .classification(ParsedFileClassification.TAX_INCOME)
                    .build();

            parsedFileAnalysis.setFile(context.getDfFile());
            parsedFileAnalysisRepository.save(parsedFileAnalysis);
            context.getDfFile().setParsedFileAnalysis(parsedFileAnalysis);
            fileRepository.save(context.getDfFile());
        } catch (Exception e) {
            log.warn("Tax income not recognized try tax income leaf parsing fileId: " + context.getDfFile().getId(), e);
            // try other accepted: tax income leaf
            try {
                TaxIncomeLeaf parsedDocument = taxIncomeLeafParser.parse(context.getFile());
                ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder()
                        .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                        .parsedFile(parsedDocument)
                        .classification(ParsedFileClassification.TAX_INCOME_LEAF)
                        .build();
                parsedFileAnalysis.setFile(context.getDfFile());
                parsedFileAnalysisRepository.save(parsedFileAnalysis);
                context.getDfFile().setParsedFileAnalysis(parsedFileAnalysis);
                fileRepository.save(context.getDfFile());
            } catch (Exception ex) {
                log.error("Tax income and tax income leaf parsing failed - fileId: " + context.getDfFile().getId(), ex);
            }
        }

        return context;
    }
}
