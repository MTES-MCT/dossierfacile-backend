package fr.dossierfacile.process.file.service.monfranceconnect.validation;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.MonFranceConnectValidationStatus;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerificationRequest;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.service.monfranceconnect.client.MonFranceConnectClient;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static fr.dossierfacile.common.enums.MonFranceConnectValidationStatus.UNKNOWN_DOCUMENT;
import static fr.dossierfacile.common.enums.MonFranceConnectValidationStatus.of;

@Slf4j
@Service
@AllArgsConstructor
public class FileValidator {

    private final MonFranceConnectClient mfcClient;

    public Optional<ValidationResult> validate(File file, InMemoryPdfFile inMemoryPdfFile, QrCode qrCode) {
        return DocumentVerificationRequest.forDocumentWith(qrCode)
                .map(verificationRequest -> mfcClient.verifyDocument(verificationRequest)
                        .map(verifiedContent -> compareContents(inMemoryPdfFile, file, qrCode, verifiedContent))
                        .orElseGet(() -> ValidationResult.error(file, qrCode)));
    }

    private ValidationResult compareContents(InMemoryPdfFile inMemoryPdfFile, File file, QrCode qrCode, DocumentVerifiedContent content) {
        if (content.isDocumentUnknown()) {
            return new ValidationResult(file, content, qrCode, UNKNOWN_DOCUMENT);
        }

        boolean isDocumentValid = content.isMatchingWith(inMemoryPdfFile.readContentAsString());
        log.info("MFC file with ID {} is {}matching with data from API", file.getId(), isDocumentValid ? "" : "NOT ");

        MonFranceConnectValidationStatus status = of(isDocumentValid);
        return new ValidationResult(file, content, qrCode, status);
    }

}
