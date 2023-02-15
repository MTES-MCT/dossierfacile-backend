package fr.dossierfacile.process.file.service.monfranceconnect.validation;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.process.file.service.interfaces.ApiTesseract;
import fr.dossierfacile.process.file.service.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.service.monfranceconnect.client.MonFranceConnectClient;
import fr.dossierfacile.process.file.util.QrCode;
import fr.dossierfacile.process.file.util.Utility;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FileValidator {

    private final MonFranceConnectClient mfcClient;
    private final Utility utility;
    private final ApiTesseract apiTesseract;

    public List<ValidationResult> validate(List<File> files) {
        return files.stream()
                .filter(file -> FilenameUtils.getExtension(file.getPath()).equals("pdf"))
                .map(this::validateDocumentIfMonFranceConnect)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<ValidationResult> validateDocumentIfMonFranceConnect(File file) {
        return utility.extractQrCode(file)
                .flatMap(qrCode -> mfcClient.fetchDocumentContent(qrCode)
                        .map(content -> buildResult(file, content, qrCode)));
    }

    private ValidationResult buildResult(File file, DocumentVerifiedContent content, QrCode qrCode) {
        boolean isDocumentValid = isActualContentMatchingWithVerifiedContent(file, content);
        log.info("MFC document with ID {} is {}matching with data from API", file.getId(), isDocumentValid ? "" : "NOT ");
        return new ValidationResult(file, content, qrCode, isDocumentValid);
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
