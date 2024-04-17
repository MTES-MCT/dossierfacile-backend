package fr.dossierfacile.process.file.service.processors;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.ParsedFileAnalysis;
import fr.dossierfacile.common.entity.ocr.ParsedFile;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.repository.ParsedFileAnalysisRepository;
import fr.dossierfacile.process.file.repository.FileRepository;
import fr.dossierfacile.process.file.service.StorageFileLoaderService;
import fr.dossierfacile.process.file.service.parsers.FileParser;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class FileParserProcessor implements Processor {
    private StorageFileLoaderService storageFileLoaderService;
    private final FileRepository fileRepository;
    private final ParsedFileAnalysisRepository parsedFileAnalysisRepository;

    private final List<FileParser<? extends ParsedFile>> fileParsers;

    @PostConstruct
    public void init() {
        fileParsers.sort(Comparator.comparingInt(parser -> {
            Order order = AnnotationUtils.findAnnotation(parser.getClass(), Order.class);
            return order == null ? Integer.MAX_VALUE : order.value();
        }));
    }

    /**
     * Gets configured parsers list for the specified type of dffile
     */
    private List<FileParser<? extends ParsedFile>> getParsers(File file) {
        return fileParsers.stream().filter(parser -> parser.shouldTryToApply(file)).toList();
    }

    public File process(File dfFile) {

        List<FileParser<?>> parsers = getParsers(dfFile);
        if (CollectionUtils.isEmpty(parsers)) {
            log.debug("There is not parser associated to this kind of document");
            return dfFile;
        }

        java.io.File file = storageFileLoaderService.getTemporaryFilePath(dfFile.getStorageFile());
        if (file == null) {
            log.error("File reading Error");
            return dfFile;
        }
        try {
            for (FileParser parser : parsers) {
                try {
                    ParsedFile parsedDocument = parser.parse(file);
                    if (parsedDocument == null) {
                        log.warn("File {} has not been parsed", dfFile.getId());
                    } else {
                        ParsedFileAnalysis parsedFileAnalysis = ParsedFileAnalysis.builder()
                                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                                .parsedFile(parsedDocument)
                                .file(dfFile)
                                .classification(parsedDocument.getClassification())
                                .build();

                        parsedFileAnalysisRepository.save(parsedFileAnalysis);
                        dfFile.setParsedFileAnalysis(parsedFileAnalysis);
                        fileRepository.save(dfFile);
                        log.info("Successfully parse file {}", dfFile.getId());
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Unable to parse file {}", dfFile.getId(), e);
                }
            }
        } finally {
            storageFileLoaderService.removeFileIfExist(file);
        }
        return dfFile;
    }
}
