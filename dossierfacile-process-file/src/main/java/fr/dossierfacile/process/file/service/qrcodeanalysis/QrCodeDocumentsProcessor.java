package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.repository.QrCodeFileAnalysisRepository;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.FileAuthenticator;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.MonFranceConnectDocumentType;
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
import static fr.dossierfacile.common.enums.QrCodeFileStatus.WRONG_CATEGORY;

@Slf4j
@Service
@AllArgsConstructor
public class QrCodeDocumentsProcessor {

    private static final List<DocumentCategory> CATEGORIES_TO_PROCESS = List.of(TAX, FINANCIAL, PROFESSIONAL);
    private final String PDF_TYPE = "application/pdf";

    private final FileAuthenticator fileAuthenticator;
    private final QrCodeFileAnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;

    public void process(Documents documents) {
        List<Document> documentsToProcess = documents.byCategories(CATEGORIES_TO_PROCESS);

        for (Document document : documentsToProcess) {
            document.getFiles().stream()
                    .filter(this::hasNotAlreadyBeenProcessed)
                    .filter(file -> PDF_TYPE.equals(file.getContentType()))
                    .forEach(file -> downloadAndValidate(file, document));
        }
    }

    private boolean hasNotAlreadyBeenProcessed(File file) {
        boolean resultExists = analysisRepository.existsByFileId(file.getId());
        return !resultExists;
    }

    private void downloadAndValidate(File file, Document document) {
        try (InMemoryPdfFile inMemoryPdfFile = InMemoryPdfFile.create(file, fileStorageService)) {
            inMemoryPdfFile.findQrCode()
                    .ifPresent(qrCode -> validateFile(inMemoryPdfFile, qrCode, document)
                            .ifPresent(result -> saveResult(result, file, qrCode)));
        } catch (IOException e) {
            log.error("Unable to download file " + file.getPath(), e);
            Sentry.captureMessage("Unable to download file " + file.getPath());
        }
    }

    private Optional<AuthenticationResult> validateFile(InMemoryPdfFile inMemoryPdfFile, QrCode qrCode, Document document) {
        boolean isAllowedInCurrentCategory = MonFranceConnectDocumentType.of(inMemoryPdfFile)
                .getCategory()
                .map(guess -> guess.isMatchingCategoryOf(document))
                .orElse(true);
        if (!isAllowedInCurrentCategory) {
            return Optional.of(new AuthenticationResult(null, WRONG_CATEGORY));
        }

        return fileAuthenticator.authenticate(inMemoryPdfFile, qrCode);
    }

    private void saveResult(AuthenticationResult authenticationResult, File file, QrCode qrCode) {
        QrCodeFileAnalysis qrCodeFileAnalysis = authenticationResult.toAnalysisResult(file, qrCode);
        analysisRepository.save(qrCodeFileAnalysis);
    }

}
