package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.barcode.InMemoryFile;
import fr.dossierfacile.process.file.repository.BarCodeFileAnalysisRepository;
import fr.dossierfacile.process.file.service.DocumentClassifier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class BarCodeFileProcessor {

    private final QrCodeFileAuthenticator qrCodeFileAuthenticator;
    private final TwoDDocFileAuthenticator twoDDocFileAuthenticator;

    private final BarCodeFileAnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;

    public void process(File file) {
        if (analysisRepository.hasNotAlreadyBeenAnalyzed(file)) {
            long start = System.currentTimeMillis();
            log.info("Starting analysis of file");
            downloadAndAnalyze(file)
                    .ifPresent(analysis -> save(file, analysis));
            log.info("Analysis of file finished in {} ms", System.currentTimeMillis() - start);
        }
    }

    private Optional<BarCodeFileAnalysis> downloadAndAnalyze(File file) {
        try (InMemoryFile inMemoryFile = InMemoryFile.download(file, fileStorageService)) {
            return analyze(inMemoryFile)
                    .map(analysis -> {
                        boolean isAllowed = new DocumentClassifier(analysis.getDocumentType()).isCompatibleWith(file);
                        analysis.setAllowedInDocumentCategory(isAllowed);
                        return analysis;
                    });
        } catch (Exception e) {
            log.error("Unable to download file", e);
        }
        return Optional.empty();
    }

    private Optional<BarCodeFileAnalysis> analyze(InMemoryFile file) {
        if (file.hasQrCode()) {
            return qrCodeFileAuthenticator.analyze(file);
        }

        if (file.has2DDoc()) {
            return Optional.of(twoDDocFileAuthenticator.analyze(file.get2DDoc()));
        }

        return Optional.empty();
    }

    private void save(File file, BarCodeFileAnalysis analysis) {
        analysis.setFile(file);
        analysisRepository.save(analysis);
    }

}
