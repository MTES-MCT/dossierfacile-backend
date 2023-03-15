package fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect;

import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import fr.dossierfacile.process.file.service.qrcodeanalysis.AuthenticationResult;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client.DocumentVerificationRequest;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client.MonFranceConnectClient;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static fr.dossierfacile.common.enums.FileAuthenticationStatus.API_ERROR;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.UNKNOWN_DOCUMENT;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.of;

@Slf4j
@Service
@AllArgsConstructor
public class FileAuthenticator {

    private final MonFranceConnectClient mfcClient;

    public Optional<AuthenticationResult> authenticate(InMemoryPdfFile inMemoryPdfFile, QrCode qrCode) {
        return DocumentVerificationRequest.forDocumentWith(qrCode)
                .map(verificationRequest -> mfcClient.verifyDocument(verificationRequest)
                        .map(verifiedContent -> compareContents(inMemoryPdfFile, verifiedContent))
                        .orElseGet(() -> new AuthenticationResult(null, API_ERROR)));
    }

    private AuthenticationResult compareContents(InMemoryPdfFile inMemoryPdfFile, DocumentVerifiedContent content) {
        if (content.isDocumentUnknown()) {
            return new AuthenticationResult(content, UNKNOWN_DOCUMENT);
        }

        boolean isAuthentic = content.isMatchingWith(inMemoryPdfFile.readContentAsString());

        FileAuthenticationStatus status = of(isAuthentic);
        return new AuthenticationResult(content, status);
    }

}
