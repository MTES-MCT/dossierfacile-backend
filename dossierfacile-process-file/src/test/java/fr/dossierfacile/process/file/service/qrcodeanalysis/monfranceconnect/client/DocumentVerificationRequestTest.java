package fr.dossierfacile.process.file.service.qrcodeanalysis.monfranceconnect.client;

import fr.dossierfacile.process.file.util.QrCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentVerificationRequestTest {

    private static final QrCode MFC_QR_CODE = new QrCode("https://mfc.fr/verif-justificatif?id=ecbfd395-b77a-44ca-9626-efb3898424a8&data=6juDifWw5wQJzqff0ZZae8B3WZwyfQE2ufLFetiXTJKjSnKApiQ70DeXfvStUf8WCYam4OixOl3dR%2BB4G6WjiGx0zUGSwuCWPSy7ubNUKyD2TKh5VvWgOjs8c%2BxICcjvk0BY%2FfWhs41IvA%3D%3D");

    @Test
    void should_build_uri_to_call() {
        DocumentVerificationRequest request = DocumentVerificationRequest.forDocumentWith(MFC_QR_CODE).get();

        assertThat(request.getUri("https://api.mfc.fr"))
                .hasScheme("https")
                .hasHost("api.mfc.fr")
                .hasPath("/verif-justificatif")
                .hasParameter("idJustificatif", "ecbfd395-b77a-44ca-9626-efb3898424a8");
    }

    @Test
    void should_build_http_entity() {
        DocumentVerificationRequest request = DocumentVerificationRequest.forDocumentWith(MFC_QR_CODE).get();
        HttpEntity<String> httpEntity = request.getHttpEntity();

        assertThat(httpEntity.getHeaders())
                .containsEntry("Content-Type", List.of("application/json"));
        assertThat(httpEntity.getBody())
                .isEqualTo("6juDifWw5wQJzqff0ZZae8B3WZwyfQE2ufLFetiXTJKjSnKApiQ70DeXfvStUf8WCYam4OixOl3dR+B4G6WjiGx0zUGSwuCWPSy7ubNUKyD2TKh5VvWgOjs8c+xICcjvk0BY/fWhs41IvA==");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://not.mfc.fr/verif-justificatif?id=aaa",
            "https://not.mfc.fr/verif-justificatif?data=aaa",
            "https://not.mfc.fr/verif-justificatif?param=aaa",
            "https://not.mfc.fr/some-path?id=aaa&data=bbb",
    })
    void should_not_build_request_if_the_qr_code_is_not_from_MonFranceConnect(String qrCodeContent) {
        QrCode qrCode = new QrCode(qrCodeContent);
        assertThat(DocumentVerificationRequest.forDocumentWith(qrCode)).isEmpty();
    }

}