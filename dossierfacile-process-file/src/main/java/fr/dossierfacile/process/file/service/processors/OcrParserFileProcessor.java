package fr.dossierfacile.process.file.service.processors;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.repository.ParsedFileAnalysisRepository;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.ocr.GuaranteeVisaleParser;
import fr.dossierfacile.process.file.service.ocr.OcrParser;
import fr.dossierfacile.process.file.service.ocr.TaxIncomeLeafParser;
import fr.dossierfacile.process.file.service.ocr.TaxIncomeParser;
import fr.dossierfacile.process.file.service.StorageFileLoaderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_GUARANTEE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.VISALE;

@Slf4j
@Service
@AllArgsConstructor
public class OcrParserFileProcessor implements Processor {
    private StorageFileLoaderService storageFileLoaderService;
    private final FileRepository fileRepository;
    private final ParsedFileAnalysisRepository parsedFileAnalysisRepository;
    private final TaxIncomeParser taxIncomeParser;
    private final TaxIncomeLeafParser taxIncomeLeafParser;
    private final GuaranteeVisaleParser guaranteeVisaleParser;

    /**
     * Gets configured parsers list for the specified type of dffile
     */
    private List<OcrParser> getParsers(File file) {
        if (file.getDocument().getDocumentCategory() == DocumentCategory.TAX) {
            return Arrays.asList(taxIncomeParser, taxIncomeLeafParser);
        }
        if (file.getDocument().getDocumentCategory() == DocumentCategory.IDENTIFICATION
                && List.of(OTHER_GUARANTEE, VISALE).contains(file.getDocument().getDocumentSubCategory()))
            return Collections.singletonList(guaranteeVisaleParser);
        return null;
    }

    public File process(File dfFile) {

        List<OcrParser> parsers = getParsers(dfFile);
        if (CollectionUtils.isEmpty(parsers)) {
            log.debug("There is not parser associateed to this kind of document");
            return dfFile;
        }

        java.io.File file = storageFileLoaderService.getTemporaryFilePath(dfFile.getStorageFile());
        if (file == null) {
            log.error("File reading Error");
            return dfFile;
        }
        try {
            for (OcrParser parser : parsers) {
                try {
                    ParsedFile parsedDocument = parser.parse(file);
                    ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder()
                            .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                            .parsedFile(parsedDocument)
                            .classification(parsedDocument.getClassification())
                            .build();

                    parsedFileAnalysis.setFile(dfFile);
                    parsedFileAnalysisRepository.save(parsedFileAnalysis);
                    dfFile.setParsedFileAnalysis(parsedFileAnalysis);
                    fileRepository.save(dfFile);
                    log.info("Successfully parse file {}", dfFile.getId());
                    break;
                } catch (Exception e) {
                    log.warn("Unable to parse file {}", dfFile.getId());
                }
            }
        } finally {
            storageFileLoaderService.removeFileIfExist(file);
        }
        return dfFile;
    }
}
