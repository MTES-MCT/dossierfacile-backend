package fr.dossierfacile.process.file.service.monfranceconnect.validation;

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
public class FileAuthenticator {

    private final MonFranceConnectClient mfcClient;

    public Optional<ValidationResult> authenticate(InMemoryPdfFile inMemoryPdfFile, QrCode qrCode) {
        return DocumentVerificationRequest.forDocumentWith(qrCode)
                .map(verificationRequest -> mfcClient.verifyDocument(verificationRequest)
                        .map(verifiedContent -> compareContents(inMemoryPdfFile, verifiedContent))
                        .orElseGet(ValidationResult::error));
    }

    private ValidationResult compareContents(InMemoryPdfFile inMemoryPdfFile, DocumentVerifiedContent content) {
        if (content.isDocumentUnknown()) {
            return new ValidationResult(content, UNKNOWN_DOCUMENT);
        }

        boolean isDocumentValid = content.isMatchingWith(inMemoryPdfFile.readContentAsString());

        MonFranceConnectValidationStatus status = of(isDocumentValid);
        return new ValidationResult(content, status);
    }

}
