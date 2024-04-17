package fr.dossierfacile.process.file.service.qrcodeanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.BarCodeType;
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
    private final ObjectMapper objectMapper;
    private final TwoDDocCertificationAuthorities certificationAuthorities;

    public BarCodeFileAnalysis analyze(TwoDDocRawContent twoDDocContent) {
        TwoDDoc twoDDoc = TwoDDocC40Parser.parse(twoDDocContent);
        FileAuthenticationStatus status = authenticate(twoDDoc);
        log.info("2D-Doc authenticity check result: {}", status);

        return BarCodeFileAnalysis.builder()
                .documentType(twoDDoc.getDocumentType())
                .barCodeContent(twoDDocContent.rawContent())
                .verifiedData(objectMapper.convertValue(twoDDoc.data().withLabels(), ObjectNode.class))
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
        X509Certificate signingCertificate = certificationAuthorities.getCertificateUsedFor(twoDDoc.header());
        return twoDDoc.isSignedBy(signingCertificate);
    }

}
