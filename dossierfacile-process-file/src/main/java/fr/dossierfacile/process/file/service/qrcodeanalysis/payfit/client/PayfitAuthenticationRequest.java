package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client;

import fr.dossierfacile.process.file.service.qrcodeanalysis.AuthenticationRequest;
import fr.dossierfacile.process.file.util.QrCode;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class PayfitAuthenticationRequest implements AuthenticationRequest {

    private final String tokenId;
    private final String secret;
    private final String pin;

    public static Optional<PayfitAuthenticationRequest> forDocumentWith(QrCode qrCode, String content) {
        Pattern payfitUrlPattern = Pattern.compile("verify\\.payfit\\.com/(\\w+)-(\\w+)");
        Matcher matcher = payfitUrlPattern.matcher(qrCode.getContent());
        if (matcher.find()) {
            String tokenId = matcher.group(1);
            String secret = matcher.group(2);
            return findVerificationCode(content)
                    .map(code -> new PayfitAuthenticationRequest(tokenId, secret, code));
        }
        return Optional.empty();
    }

    private static Optional<String> findVerificationCode(String content) {
        Pattern verificationCodePattern = Pattern.compile("CODE DE VÉRIFICATION : (\\d{6})");
        Matcher matcher = verificationCodePattern.matcher(content);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

}
