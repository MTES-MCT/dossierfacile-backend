package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.BarCodeType;
import fr.dossierfacile.process.file.barcode.qrcode.QrCode;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class QrCodeFileAuthenticator {

    private final List<QrCodeDocumentIssuer<? extends AuthenticationRequest>> issuers;

    Optional<BarCodeFileAnalysis> analyze(InMemoryPdfFile file) {
        for (QrCodeDocumentIssuer<?> issuer : issuers) {
            Optional<AuthenticationResult> result = issuer.tryToAuthenticate(file);
            if (result.isPresent()) {
                BarCodeFileAnalysis fileAnalysis = buildAnalysis(result.get(), file.getQrCode());
                return Optional.of(fileAnalysis);
            }
        }

        return Optional.empty();
    }

    private BarCodeFileAnalysis buildAnalysis(AuthenticationResult result, QrCode qrCode) {
        return BarCodeFileAnalysis.builder()
                .documentType(result.getDocumentType())
                .barCodeContent(qrCode.getContent())
                .verifiedData(result.getApiResponse())
                .authenticationStatus(result.getAuthenticationStatus())
                .barCodeType(BarCodeType.QR_CODE)
                .build();
    }

}
