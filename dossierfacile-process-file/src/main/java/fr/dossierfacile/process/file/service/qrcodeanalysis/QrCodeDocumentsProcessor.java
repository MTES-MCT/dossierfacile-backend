package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.repository.QrCodeFileAnalysisRepository;
import fr.dossierfacile.process.file.util.Documents;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import fr.dossierfacile.process.file.util.QrCode;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentCategory.FINANCIAL;
import static fr.dossierfacile.common.enums.DocumentCategory.PROFESSIONAL;
import static fr.dossierfacile.common.enums.DocumentCategory.TAX;

@Slf4j
@Service
@AllArgsConstructor
public class QrCodeDocumentsProcessor {

    private static final List<DocumentCategory> CATEGORIES_TO_PROCESS = List.of(TAX, FINANCIAL, PROFESSIONAL);
    private final String PDF_TYPE = "application/pdf";

    private final List<QrCodeDocumentIssuer<? extends AuthenticationRequest>> issuers;

    private final QrCodeFileAnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;

    public void process(Documents documents) {
        List<Document> documentsToProcess = documents.byCategories(CATEGORIES_TO_PROCESS);

        for (Document document : documentsToProcess) {
            document.getFiles().stream()
                    .filter(analysisRepository::hasNotAlreadyBeenAnalyzed)
                    .filter(file -> PDF_TYPE.equals(file.getContentType()))
                    .forEach(file -> downloadAndAnalyze(file, document)
                            .ifPresent(analysis -> save(file, analysis)));
        }
    }

    private Optional<QrCodeFileAnalysis> downloadAndAnalyze(File file, Document document) {
        try (InMemoryPdfFile inMemoryPdfFile = InMemoryPdfFile.create(file, fileStorageService)) {
            return analyze(document, inMemoryPdfFile);
        } catch (IOException e) {
            log.error("Unable to download file " + file.getPath(), e);
            Sentry.captureMessage("Unable to download file " + file.getPath());
        }
        return Optional.empty();
    }

    private Optional<QrCodeFileAnalysis> analyze(Document document, InMemoryPdfFile file) {
        if (!file.hasQrCode()) {
            return Optional.empty();
        }

        for (QrCodeDocumentIssuer<?> issuer : issuers) {
            Optional<AuthenticationResult> result = issuer.tryToAuthenticate(file);
            if (result.isPresent()) {
                boolean allowedInCurrentCategory = issuer.guessCategory(file)
                        .map(guess -> guess.isMatchingCategoryOf(document))
                        .orElse(true);
                QrCodeFileAnalysis fileAnalysis = buildAnalysis(result.get(), file.getQrCode(), allowedInCurrentCategory);
                return Optional.of(fileAnalysis);
            }
        }

        return Optional.empty();
    }

    private void save(File file, QrCodeFileAnalysis analysis) {
        analysis.setFile(file);
        analysisRepository.save(analysis);
    }

    public QrCodeFileAnalysis buildAnalysis(AuthenticationResult result, QrCode qrCode, boolean isAllowedInDocumentCategory) {
        QrCodeFileAnalysis analysis = new QrCodeFileAnalysis();
        analysis.setIssuerName(result.getIssuerName());
        analysis.setQrCodeContent(qrCode.getContent());
        analysis.setApiResponse(result.getApiResponse());
        analysis.setAuthenticationStatus(result.getAuthenticationStatus());
        analysis.setAllowedInDocumentCategory(isAllowedInDocumentCategory);
        return analysis;
    }

}
