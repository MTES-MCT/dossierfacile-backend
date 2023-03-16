package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit;

import fr.dossierfacile.common.entity.DocumentIssuer;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import fr.dossierfacile.process.file.service.qrcodeanalysis.AuthenticationResult;
import fr.dossierfacile.process.file.service.qrcodeanalysis.GuessedDocumentCategory;
import fr.dossierfacile.process.file.service.qrcodeanalysis.QrCodeDocumentIssuer;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitAuthenticationRequest;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitClient;
import fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client.PayfitResponse;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static fr.dossierfacile.common.entity.DocumentIssuer.PAYFIT;

@Service
@AllArgsConstructor
public class PayfitDocumentIssuer extends QrCodeDocumentIssuer<PayfitAuthenticationRequest> {

    private final PayfitClient client;

    @Override
    protected DocumentIssuer getName() {
        return PAYFIT;
    }

    @Override
    protected Optional<PayfitAuthenticationRequest> buildAuthenticationRequestFor(InMemoryPdfFile pdfFile) {
        QrCode qrCode = pdfFile.getQrCode();
        String content = pdfFile.readContentAsString();
        return PayfitAuthenticationRequest.forDocumentWith(qrCode, content);
    }

    @Override
    protected Optional<GuessedDocumentCategory> guessCategory(InMemoryPdfFile file) {
        GuessedDocumentCategory salary = new GuessedDocumentCategory(DocumentCategory.FINANCIAL, DocumentSubCategory.SALARY);
        return Optional.of(salary);
    }

    @Override
    protected AuthenticationResult authenticate(InMemoryPdfFile pdfFile, PayfitAuthenticationRequest authenticationRequest) {
        return client.getVerifiedContent(authenticationRequest)
                .map(response -> buildResult(pdfFile, response))
                .orElseGet(PayfitDocumentIssuer::error);
    }

    private static AuthenticationResult buildResult(InMemoryPdfFile pdfFile, PayfitResponse response) {
        String content = pdfFile.readContentAsString();
        boolean isAuthentic = PaySlipVerifiedContent.from(response).isMatchingWith(content);
        return new AuthenticationResult(PAYFIT, response, FileAuthenticationStatus.of(isAuthentic));
    }

    private static AuthenticationResult error() {
        return new AuthenticationResult(PAYFIT, null, FileAuthenticationStatus.API_ERROR);
    }

}
