package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.DocumentIssuer;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client.DocumentVerifiedContent;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString
@AllArgsConstructor
public class AuthenticationResult {

    private final DocumentIssuer issuerName = DocumentIssuer.MON_FRANCE_CONNECT;
    private final DocumentVerifiedContent content;
    private final FileAuthenticationStatus authenticationStatus;

    public QrCodeFileAnalysis toAnalysisResult(File file, QrCode qrCode, boolean isAllowedInDocumentCategory) {
        QrCodeFileAnalysis analysis = new QrCodeFileAnalysis();
        analysis.setFile(file);
        analysis.setIssuerName(issuerName);
        analysis.setQrCodeContent(qrCode.getContent());
        analysis.setApiResponse(Optional.ofNullable(content)
                .map(DocumentVerifiedContent::getElements)
                .orElse(null));
        analysis.setAuthenticationStatus(authenticationStatus);
        analysis.setAllowedInDocumentCategory(isAllowedInDocumentCategory);
        return analysis;
    }

}
