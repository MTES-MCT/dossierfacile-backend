package fr.dossierfacile.process.file.service.monfranceconnect;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.process.file.service.monfranceconnect.repository.ValidationResultRepository;
import fr.dossierfacile.process.file.service.monfranceconnect.validation.FileValidator;
import fr.dossierfacile.process.file.service.monfranceconnect.validation.ValidationResult;
import fr.dossierfacile.process.file.util.Documents;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import fr.dossierfacile.process.file.util.QrCode;
import io.sentry.Sentry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentCategory.FINANCIAL;
import static fr.dossierfacile.common.enums.DocumentCategory.TAX;

@Slf4j
@Service
@AllArgsConstructor
public class MonFranceConnectDocumentsProcessor {

    private static final List<DocumentCategory> CATEGORIES_TO_PROCESS = List.of(TAX, FINANCIAL);
    private final String PDF_TYPE = "application/pdf";

    private final FileValidator documentValidator;
    private final ValidationResultRepository resultRepository;
    private final FileStorageService fileStorageService;

    public void process(Documents documents) {
        documents.byCategories(CATEGORIES_TO_PROCESS)
                .stream()
                .map(Document::getFiles)
                .flatMap(Collection::stream)
                .filter(this::hasNotAlreadyBeenProcessed)
                .filter(file -> PDF_TYPE.equals(file.getContentType()))
                .forEach(file -> tryToValidate(file).ifPresent(this::saveResult));
    }

    private boolean hasNotAlreadyBeenProcessed(File file) {
        boolean resultExists = resultRepository.existsByFileId(file.getId());
        return !resultExists;
    }

    private Optional<ValidationResult> tryToValidate(File file) {
        try (InMemoryPdfFile inMemoryPdfFile = InMemoryPdfFile.create(file, fileStorageService)) {
            return validateIfQrCodeExists(file, inMemoryPdfFile);
        } catch (IOException e) {
            log.error("Unable to download file " + file.getPath(), e);
            Sentry.captureMessage("Unable to download file " + file.getPath());
        }
        return Optional.empty();
    }

    private Optional<ValidationResult> validateIfQrCodeExists(File file, InMemoryPdfFile inMemoryPdfFile) {
        Optional<QrCode> qrCode = inMemoryPdfFile.findQrCode();
        if (qrCode.isEmpty()) {
            log.info("File {} is not issued by MonFranceConnect", file.getId());
            return Optional.empty();
        }
        return documentValidator.validate(file, inMemoryPdfFile, qrCode.get());
    }

    private void saveResult(ValidationResult validationResult) {
        resultRepository.save(validationResult.toEntity());
    }

}
