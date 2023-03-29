package fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect;

import fr.dossierfacile.common.entity.DocumentIssuer;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import fr.dossierfacile.process.file.service.qrcodeanalysis.AuthenticationResult;
import fr.dossierfacile.process.file.service.qrcodeanalysis.GuessedDocumentCategory;
import fr.dossierfacile.process.file.service.qrcodeanalysis.QrCodeDocumentIssuer;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client.MonFranceConnectAuthenticationRequest;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client.MonFranceConnectClient;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static fr.dossierfacile.common.entity.DocumentIssuer.MON_FRANCE_CONNECT;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.API_ERROR;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.UNKNOWN_DOCUMENT;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.of;

@Service
@AllArgsConstructor
public class MonFranceConnectDocumentIssuer extends QrCodeDocumentIssuer<MonFranceConnectAuthenticationRequest> {

    private final MonFranceConnectClient mfcClient;

    @Override
    protected DocumentIssuer getName() {
        return MON_FRANCE_CONNECT;
    }

    @Override
    protected Optional<MonFranceConnectAuthenticationRequest> buildAuthenticationRequestFor(InMemoryPdfFile pdfFile) {
        QrCode qrCode = pdfFile.getQrCode();
        return MonFranceConnectAuthenticationRequest.forDocumentWith(qrCode);
    }

    @Override
    protected Optional<GuessedDocumentCategory> guessCategory(InMemoryPdfFile pdfFile) {
        return MonFranceConnectDocumentType.of(pdfFile).getCategory();
    }

    @Override
    protected AuthenticationResult authenticate(InMemoryPdfFile pdfFile, MonFranceConnectAuthenticationRequest authenticationRequest) {
        return mfcClient.getVerifiedContent(authenticationRequest)
                        .map(verifiedContent -> compareContents(pdfFile, verifiedContent))
                        .orElseGet(MonFranceConnectDocumentIssuer::error);
    }

    private AuthenticationResult compareContents(InMemoryPdfFile pdfFile, DocumentVerifiedContent content) {
        if (content.isDocumentUnknown()) {
            return authenticationResult(content, UNKNOWN_DOCUMENT);
        }

        boolean isAuthentic = content.isMatchingWith(pdfFile.getContentAsString());

        FileAuthenticationStatus status = of(isAuthentic);
        return authenticationResult(content, status);
    }

    private static AuthenticationResult authenticationResult(DocumentVerifiedContent content, FileAuthenticationStatus status) {
        return new AuthenticationResult(MON_FRANCE_CONNECT, content.getElements(), status);
    }

    private static AuthenticationResult error() {
        return authenticationResult(null, API_ERROR);
    }

}
