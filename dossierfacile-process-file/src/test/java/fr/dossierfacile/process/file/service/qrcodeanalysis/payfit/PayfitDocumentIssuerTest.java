package fr.dossierfacile.process.file.service.qrcodeanalysis.payfit;

import fr.dossierfacile.process.file.IntegrationTest;
import fr.dossierfacile.process.file.TestFilesUtil;
import fr.dossierfacile.process.file.service.qrcodeanalysis.AuthenticationResult;
import fr.dossierfacile.process.file.barcode.InMemoryPdfFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static fr.dossierfacile.common.entity.BarCodeDocumentType.PAYFIT_PAYSLIP;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.API_ERROR;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.VALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@Disabled
@IntegrationTest
class PayfitDocumentIssuerTest {

    @Autowired
    private PayfitDocumentIssuer payfit;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void should_authenticate_payfit_document() throws IOException, URISyntaxException {
        InMemoryPdfFile pdfFile = TestFilesUtil.getPdfFile("fake-payfit.pdf");

        mockPayfitApi(HttpStatus.CREATED, payfitResponseBody());

        Optional<AuthenticationResult> authenticationResult = payfit.tryToAuthenticate(pdfFile);

        assertThat(authenticationResult).isPresent()
                .hasValueSatisfying(result -> assertAll(
                        () -> assertThat(result.getDocumentType()).isEqualTo(PAYFIT_PAYSLIP),
                        () -> assertThat(result.getAuthenticationStatus()).isEqualTo(VALID),
                        () -> assertThat(result.getApiResponse()).isInstanceOf(PaySlipVerifiedContent.class)
                ));
    }

    @Test
    void should_return_error_when_failing_to_get_a_response() throws IOException, URISyntaxException {
        InMemoryPdfFile pdfFile = TestFilesUtil.getPdfFile("fake-payfit.pdf");

        mockPayfitApi(HttpStatus.BAD_REQUEST, "{\"error\": \"invalid_token\"}");

        Optional<AuthenticationResult> authenticationResult = payfit.tryToAuthenticate(pdfFile);

        assertThat(authenticationResult).isPresent()
                .hasValueSatisfying(result -> assertAll(
                        () -> assertThat(result.getDocumentType()).isEqualTo(PAYFIT_PAYSLIP),
                        () -> assertThat(result.getAuthenticationStatus()).isEqualTo(API_ERROR),
                        () -> assertThat(result.getApiResponse()).isNull()
                ));
    }

    @Test
    void should_return_empty_when_document_is_not_issued_by_payfit() throws IOException {
        InMemoryPdfFile pdfFile = TestFilesUtil.getPdfFile("qr-code.pdf");

        Optional<AuthenticationResult> authenticationResult = payfit.tryToAuthenticate(pdfFile);

        assertThat(authenticationResult).isEmpty();
    }

    private void mockPayfitApi(HttpStatus status, String responseBody) throws URISyntaxException {
        mockServer.expect(once(), requestTo(new URI("https://payfit-test.com")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody)
                );
    }

    private String payfitResponseBody() {
        return """
                {
                    "content": {
                        "companyInfo": [
                            {
                                "label": "SIRET",
                                "order": 1,
                                "value": "111122223333"
                            },
                            {
                                "label": "Entreprise",
                                "order": 1,
                                "value": "E Corp"
                            },
                            {
                                "label": "Employé",
                                "order": 1,
                                "value": "Elliot Alderson"
                            }
                        ],
                        "employeeInfo": [
                            {
                                "label": "Net à payer avant impôt",
                                "order": 1,
                                "value": "12345.67"
                            },
                            {
                                "label": "Salaire brut",
                                "order": 1,
                                "value": "123456.78"
                            }
                        ]
                    },
                    "country": "FR",
                    "header": "Période du 01/03/2023 au 31/03/2023"
                }
                """;
    }

}