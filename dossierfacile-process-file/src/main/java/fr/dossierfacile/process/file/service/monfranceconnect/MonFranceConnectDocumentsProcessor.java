package fr.dossierfacile.process.file.service.monfranceconnect;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.service.monfranceconnect.repository.ValidationResultRepository;
import fr.dossierfacile.process.file.service.monfranceconnect.validation.FileAuthenticator;
import fr.dossierfacile.process.file.service.monfranceconnect.validation.ValidationResult;
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
public class MonFranceConnectDocumentsProcessor {

    private static final List<DocumentCategory> CATEGORIES_TO_PROCESS = List.of(TAX, FINANCIAL, PROFESSIONAL);
    private final String PDF_TYPE = "application/pdf";

    private final FileAuthenticator documentValidator;
    private final ValidationResultRepository resultRepository;
    private final FileStorageService fileStorageService;

    public void process(Documents documents) {
        List<Document> documentsToProcess = documents.byCategories(CATEGORIES_TO_PROCESS);

        for (Document document : documentsToProcess) {
            document.getFiles().stream()
                    .filter(this::hasNotAlreadyBeenProcessed)
                    .filter(file -> PDF_TYPE.equals(file.getContentType()))
                    .forEach(file -> downloadAndValidate(file, document).ifPresent(this::saveResult));
        }
    }

    private boolean hasNotAlreadyBeenProcessed(File file) {
        boolean resultExists = resultRepository.existsByFileId(file.getId());
        return !resultExists;
    }

    private Optional<ValidationResult> downloadAndValidate(File file, Document document) {
        try (InMemoryPdfFile inMemoryPdfFile = InMemoryPdfFile.create(file, fileStorageService)) {
            return validateFile(file, inMemoryPdfFile, document);
        } catch (IOException e) {
            log.error("Unable to download file " + file.getPath(), e);
            Sentry.captureMessage("Unable to download file " + file.getPath());
        }
        return Optional.empty();
    }

    private Optional<ValidationResult> validateFile(File file, InMemoryPdfFile inMemoryPdfFile, Document document) {
        Optional<QrCode> qrCode = inMemoryPdfFile.findQrCode();
        if (qrCode.isEmpty()) {
            log.info("File {} is not issued by MonFranceConnect", file.getId());
            return Optional.empty();
        }

        GuessedDocumentType guessedType = GuessedDocumentType.of(inMemoryPdfFile);
        boolean isInWrongCategory = !guessedType.isMatchingCategoryOf(document);
        if (isInWrongCategory) {
            return Optional.of(ValidationResult.wrongCategory(file, qrCode.get()));
        }

        return documentValidator.authenticate(file, inMemoryPdfFile, qrCode.get());
    }

    private void saveResult(ValidationResult validationResult) {
        resultRepository.save(validationResult.toEntity());
    }

}
