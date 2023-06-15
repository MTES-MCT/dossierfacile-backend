package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.BarCodeType;
import fr.dossierfacile.common.entity.DocumentIssuer;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDoc;
import fr.dossierfacile.process.file.barcode.twoddoc.TwoDDocRawContent;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocC40Parser;
import fr.dossierfacile.process.file.barcode.twoddoc.validation.TwoDDocCertificationAuthorities;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

@Slf4j
@Service
@AllArgsConstructor
public class TwoDDocFileAuthenticator {

    private final TwoDDocCertificationAuthorities certificationAuthorities;

    public BarCodeFileAnalysis analyze(TwoDDocRawContent twoDDocContent) {
        TwoDDoc twoDDoc = TwoDDocC40Parser.parse(twoDDocContent);
        DocumentIssuer issuer = twoDDoc.getIssuer();
        FileAuthenticationStatus status = authenticate(twoDDoc);

        return BarCodeFileAnalysis.builder()
                .issuerName(issuer)
                .barCodeContent(twoDDocContent.rawContent())
                .verifiedData(twoDDoc.data().withLabels())
                .authenticationStatus(status)
                .barCodeType(BarCodeType.TWO_D_DOC)
                .build();
    }

    private FileAuthenticationStatus authenticate(TwoDDoc twoDDoc) {
        try {
            return FileAuthenticationStatus.of(isAuthentic(twoDDoc));
        } catch (InvalidKeyException | SignatureException e) {
            log.error(e.getMessage(), e);
            return FileAuthenticationStatus.ERROR;
        }
    }

    private boolean isAuthentic(TwoDDoc twoDDoc) throws InvalidKeyException, SignatureException {
        X509Certificate signingCertificate = certificationAuthorities.getCertificateOf(twoDDoc.header().certId());
        return twoDDoc.isSignedBy(signingCertificate);
    }

}
