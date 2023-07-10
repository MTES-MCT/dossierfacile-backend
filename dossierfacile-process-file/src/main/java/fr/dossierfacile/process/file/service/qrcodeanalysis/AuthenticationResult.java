package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.enums.FileAuthenticationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class AuthenticationResult {

    private final BarCodeDocumentType documentType;
    private final Object apiResponse;
    private final FileAuthenticationStatus authenticationStatus;

}
