package fr.dossierfacile.process.file.service.qrcodeanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.BarCodeType;
import fr.dossierfacile.process.file.barcode.qrcode.QrCode;
import fr.dossierfacile.process.file.barcode.InMemoryFile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class QrCodeFileAuthenticator {
    private final ObjectMapper objectMapper;
    private final List<QrCodeDocumentIssuer<? extends AuthenticationRequest>> issuers;

    public Optional<BarCodeFileAnalysis> analyze(InMemoryFile file) {
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
                .verifiedData(objectMapper.convertValue(result.getApiResponse(), ObjectNode.class))
                .authenticationStatus(result.getAuthenticationStatus())
                .barCodeType(BarCodeType.QR_CODE)
                .build();
    }

}
