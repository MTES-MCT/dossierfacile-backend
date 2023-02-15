package fr.dossierfacile.process.file.service.monfranceconnect.client;

import fr.dossierfacile.process.file.util.QrCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentVerificationRequestTest {

    private static final QrCode QR_CODE = new QrCode("https://mfc.fr/verif-justificatif?id=ecbfd395-b77a-44ca-9626-efb3898424a8&data=6juDifWw5wQJzqff0ZZae8B3WZwyfQE2ufLFetiXTJKjSnKApiQ70DeXfvStUf8WCYam4OixOl3dR%2BB4G6WjiGx0zUGSwuCWPSy7ubNUKyD2TKh5VvWgOjs8c%2BxICcjvk0BY%2FfWhs41IvA%3D%3D");

    @Test
    void should_build_uri_to_call() {
        DocumentVerificationRequest request = DocumentVerificationRequest.forDocumentWith(QR_CODE);

        assertThat(request.getUri("https://api.mfc.fr"))
                .hasScheme("https")
                .hasHost("api.mfc.fr")
                .hasPath("/verif-justificatif")
                .hasParameter("idJustificatif", "ecbfd395-b77a-44ca-9626-efb3898424a8");
    }

    @Test
    void should_build_http_entity() {
        DocumentVerificationRequest request = DocumentVerificationRequest.forDocumentWith(QR_CODE);
        HttpEntity<String> httpEntity = request.getHttpEntity();

        assertThat(httpEntity.getHeaders())
                .containsEntry("Content-Type", List.of("application/json"));
        assertThat(httpEntity.getBody())
                .isEqualTo("6juDifWw5wQJzqff0ZZae8B3WZwyfQE2ufLFetiXTJKjSnKApiQ70DeXfvStUf8WCYam4OixOl3dR+B4G6WjiGx0zUGSwuCWPSy7ubNUKyD2TKh5VvWgOjs8c+xICcjvk0BY/fWhs41IvA==");
    }
}