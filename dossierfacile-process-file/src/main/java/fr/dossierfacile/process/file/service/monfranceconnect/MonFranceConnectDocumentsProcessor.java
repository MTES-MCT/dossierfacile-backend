package fr.dossierfacile.process.file.service.monfranceconnect;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.process.file.service.monfranceconnect.repository.ValidationResultRepository;
import fr.dossierfacile.process.file.service.monfranceconnect.validation.FileValidator;
import fr.dossierfacile.process.file.service.monfranceconnect.validation.ValidationResult;
import fr.dossierfacile.process.file.util.Documents;
import fr.dossierfacile.process.file.util.QrCode;
import fr.dossierfacile.process.file.util.Utility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static fr.dossierfacile.common.enums.DocumentCategory.*;

@Slf4j
@Service
@AllArgsConstructor
public class MonFranceConnectDocumentsProcessor {

    private static final List<DocumentCategory> CATEGORIES_TO_PROCESS = List.of(TAX, FINANCIAL);
    private final String PDF_TYPE = "application/pdf";

    private final FileValidator documentValidator;
    private final Utility utility;
    private final ValidationResultRepository resultRepository;

    public void process(Documents documents) {
        documents.byCategories(CATEGORIES_TO_PROCESS)
                .stream()
                .map(Document::getFiles)
                .flatMap(Collection::stream)
                .filter(this::hasNotAlreadyBeenProcessed)
                .filter(file -> PDF_TYPE.equals(file.getContentType()))
                .forEach(file -> validate(file).ifPresent(this::saveResult));
    }

    private boolean hasNotAlreadyBeenProcessed(File file) {
        boolean resultExists = resultRepository.existsByFileId(file.getId());
        return !resultExists;
    }

    private Optional<ValidationResult> validate(File file) {
        Optional<QrCode> qrCode = utility.extractQrCode(file);
        if (qrCode.isEmpty()) {
            log.info("File {} is not issued by MonFranceConnect", file.getId());
            return Optional.empty();
        }
        return documentValidator.validate(file, qrCode.get());
    }

    private void saveResult(ValidationResult validationResult) {
        resultRepository.save(validationResult.toEntity());
    }

}
