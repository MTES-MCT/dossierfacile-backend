package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import fr.dossierfacile.common.enums.QrCodeFileStatus;
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

    private final DocumentVerifiedContent content;
    private final QrCodeFileStatus validationStatus;

    public QrCodeFileAnalysis toAnalysisResult(File file, QrCode qrCode) {
        QrCodeFileAnalysis analysis = new QrCodeFileAnalysis();
        analysis.setFile(file);
        analysis.setQrCodeContent(qrCode.getContent());
        analysis.setApiResponse(Optional.ofNullable(content)
                .map(DocumentVerifiedContent::getElements)
                .orElse(null));
        analysis.setValidationStatus(validationStatus);
        return analysis;
    }

}
