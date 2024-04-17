package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit;

import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import fr.dossierfacile.process.file.barcode.qrcode.QrCode;
import fr.dossierfacile.process.file.service.qrcodeanalysis.AuthenticationResult;
import fr.dossierfacile.process.file.service.qrcodeanalysis.QrCodeDocumentIssuer;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitAuthenticationRequest;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitClient;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitResponse;
import fr.dossierfacile.process.file.barcode.InMemoryFile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static fr.dossierfacile.common.entity.BarCodeDocumentType.PAYFIT_PAYSLIP;

@Service
@AllArgsConstructor
public class PayfitDocumentIssuer extends QrCodeDocumentIssuer<PayfitAuthenticationRequest> {

    private final PayfitClient client;

    @Override
    protected Optional<PayfitAuthenticationRequest> buildAuthenticationRequestFor(InMemoryFile file) {
        QrCode qrCode = file.getQrCode();
        String content = file.getContentAsString();
        return PayfitAuthenticationRequest.forDocumentWith(qrCode, content);
    }

    @Override
    protected AuthenticationResult authenticate(InMemoryFile file, PayfitAuthenticationRequest authenticationRequest) {
        return client.getVerifiedContent(authenticationRequest)
                .map(response -> buildResult(file, response))
                .orElseGet(this::error);
    }

    private AuthenticationResult buildResult(InMemoryFile pdfFile, PayfitResponse response) {
        PaySlipVerifiedContent verifiedContent = PaySlipVerifiedContent.from(response);
        return new AuthenticationResult(PAYFIT_PAYSLIP, verifiedContent, FileAuthenticationStatus.VALID);
    }

    private AuthenticationResult error() {
        return new AuthenticationResult(PAYFIT_PAYSLIP, null, FileAuthenticationStatus.API_ERROR);
    }

}
