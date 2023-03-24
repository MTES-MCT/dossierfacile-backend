package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit.client;

import fr.dossierfacile.process.file.util.QrCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PayfitAuthenticationRequestTest {

    private static final QrCode PAYFIT_QR_CODE = new QrCode("verify.payfit.com/63827c46091af03517d11635-b8a310fe821f19d75495");

    @Test
    void should_parse_qrcode_and_content() {
        String fileContent = "aaa CODE DE VÉRIFICATION : 123456 bbb ccc ....";

        var authenticationRequest = PayfitAuthenticationRequest.forDocumentWith(PAYFIT_QR_CODE, fileContent);

        assertThat(authenticationRequest).isPresent()
                .contains(new PayfitAuthenticationRequest("63827c46091af03517d11635", "b8a310fe821f19d75495", "123456"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "other.issuer.com/63827c46091af03517d11635-b8a310fe821f19d75495",
            "verify.payfit.com/63827c46091af03517d11635"
    })
    void should_fail_if_url_is_not_valid() {
        QrCode qrCode = new QrCode("other.issuer.com/63827c46091af03517d11635-b8a310fe821f19d75495");

        var authenticationRequest = PayfitAuthenticationRequest.forDocumentWith(qrCode, "");

        assertThat(authenticationRequest).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "aaa CODE DE VÉRIFICATION : xxx",
            "aaa bbb"
    })
    void should_fail_if_verification_code_is_not_found(String content) {
        var authenticationRequest = PayfitAuthenticationRequest.forDocumentWith(PAYFIT_QR_CODE, content);
        assertThat(authenticationRequest).isEmpty();
    }

}