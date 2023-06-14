package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.repository.QrCodeFileAnalysisRepository;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import fr.dossierfacile.process.file.barcode.qrcode.QrCode;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class QrCodeFileProcessor {

    private static final String PDF_TYPE = "application/pdf";

    private final List<QrCodeDocumentIssuer<? extends AuthenticationRequest>> issuers;

    private final QrCodeFileAnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;

    public void process(File file) {
        if (analysisRepository.hasNotAlreadyBeenAnalyzed(file) &&
                PDF_TYPE.equals(file.getStorageFile().getContentType())) {
            downloadAndAnalyze(file)
                    .ifPresent(analysis -> save(file, analysis));
        }
    }

    private Optional<QrCodeFileAnalysis> downloadAndAnalyze(File file) {
        try (InMemoryPdfFile inMemoryPdfFile = InMemoryPdfFile.create(file, fileStorageService)) {
            return analyze(inMemoryPdfFile)
                    .map(analysis -> {
                        var guess = GuessedDocumentCategory.forFile(inMemoryPdfFile, analysis.getIssuerName());
                        boolean isAllowed = guess.isMatchingCategoryOf(file.getDocument());
                        analysis.setAllowedInDocumentCategory(isAllowed);
                        return analysis;
                    });
        } catch (IOException e) {
            log.error("Unable to download file " + file.getStorageFile().getPath(), e);
            Sentry.captureMessage("Unable to download file " + file.getStorageFile().getPath());
        }
        return Optional.empty();
    }

    private Optional<QrCodeFileAnalysis> analyze(InMemoryPdfFile file) {
        if (!file.hasQrCode()) {
            return Optional.empty();
        }

        for (QrCodeDocumentIssuer<?> issuer : issuers) {
            Optional<AuthenticationResult> result = issuer.tryToAuthenticate(file);
            if (result.isPresent()) {
                QrCodeFileAnalysis fileAnalysis = buildAnalysis(result.get(), file.getQrCode());
                return Optional.of(fileAnalysis);
            }
        }

        return Optional.empty();
    }

    private void save(File file, QrCodeFileAnalysis analysis) {
        analysis.setFile(file);
        analysisRepository.save(analysis);
    }

    public QrCodeFileAnalysis buildAnalysis(AuthenticationResult result, QrCode qrCode) {
        QrCodeFileAnalysis analysis = new QrCodeFileAnalysis();
        analysis.setIssuerName(result.getIssuerName());
        analysis.setQrCodeContent(qrCode.getContent());
        analysis.setApiResponse(result.getApiResponse());
        analysis.setAuthenticationStatus(result.getAuthenticationStatus());
        analysis.setAllowedInDocumentCategory(true);
        return analysis;
    }
}
