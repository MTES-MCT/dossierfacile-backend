package fr.dossierfacile.process.file.service.monfranceconnect.validation;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.enums.MonFranceConnectValidationStatus;
import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.service.monfranceconnect.client.MonFranceConnectClient;
import fr.dossierfacile.process.file.util.QrCode;
import fr.dossierfacile.process.file.util.Utility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static fr.dossierfacile.common.enums.MonFranceConnectValidationStatus.UNKNOWN_DOCUMENT;
import static fr.dossierfacile.common.enums.MonFranceConnectValidationStatus.of;

@Slf4j
@Service
@AllArgsConstructor
public class FileValidator {

    private final MonFranceConnectClient mfcClient;
    private final Utility utility;
    private final ApiTesseract apiTesseract;

    public ValidationResult validate(File file, QrCode qrCode) {
        return mfcClient.fetchDocumentContent(qrCode)
                .map(content -> buildResult(file, qrCode, content))
                .orElseGet(() -> ValidationResult.error(file, qrCode));
    }

    private ValidationResult buildResult(File file, QrCode qrCode, DocumentVerifiedContent content) {
        if (content.isDocumentUnknown()) {
            return new ValidationResult(file, content, qrCode, UNKNOWN_DOCUMENT);
        }
        boolean isDocumentValid = isActualContentMatchingWithVerifiedContent(file, content);
        log.info("MFC document with ID {} is {}matching with data from API", file.getId(), isDocumentValid ? "" : "NOT ");
        MonFranceConnectValidationStatus status = of(isDocumentValid);
        return new ValidationResult(file, content, qrCode, status);
    }

    private boolean isActualContentMatchingWithVerifiedContent(File pdf, DocumentVerifiedContent verifiedContent) {
        String fileContent = utility.extractInfoFromPDFFirstPage(pdf);
        if (verifiedContent.isMatchingWith(fileContent)) {
            return true;
        }

        java.io.File tmpFile = utility.getTemporaryFile(pdf);
        String extractedContentWithOcr = apiTesseract.extractText(tmpFile);
        try {
            if (!tmpFile.delete()) {
                log.warn("Unable to delete file");
            }
        } catch (Exception e) {
            log.warn("Unable to delete file", e);
        }

        return verifiedContent.isMatchingWith(extractedContentWithOcr);
    }

}
