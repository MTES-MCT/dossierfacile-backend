package fr.gouv.bo.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.DocumentIssuer;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayableQrCodeFileAnalysisTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_format_mon_france_connect_content() throws JsonProcessingException {
        var analysis = buildAnalysis(DocumentIssuer.MON_FRANCE_CONNECT, """
                ["2021", "Angela Claire Louise, DUBOIS", "Philippe, DUBOIS", "Marié(e)", "20 avenue de Ségur", "82357   €", "88185   €", "2,5", "1,0"]
                """);

        var displayableAnalysis = new DisplayableQrCodeFileAnalysis(1, analysis);

        assertThat(displayableAnalysis.getAuthenticatedContent())
                .isEqualTo("2021, Angela Claire Louise, DUBOIS, Philippe, DUBOIS, Marié(e), 20 avenue de Ségur, 82357   €, 88185   €, 2,5, 1,0");
    }

    @Test
    void should_format_payfit_content() throws JsonProcessingException {
        var analysis = buildAnalysis(DocumentIssuer.PAYFIT, """
                {
                    "netSalary": "1 895,39 €",
                    "companyName": "Some Company",
                    "grossSalary": "2 533,33 €",
                    "companySiret": "128759437",
                    "employeeName": "John Doe"
                }
                """);

        var displayableAnalysis = new DisplayableQrCodeFileAnalysis(1, analysis);

        assertThat(displayableAnalysis.getAuthenticatedContent())
                .isEqualTo("entreprise = Some Company, employé = John Doe, net = 1 895,39 €");
    }

    private QrCodeFileAnalysis buildAnalysis(DocumentIssuer issuer, String content) throws JsonProcessingException {
        QrCodeFileAnalysis analysis = new QrCodeFileAnalysis();
        analysis.setIssuerName(issuer);
        analysis.setApiResponse(objectMapper.readValue(content, Object.class));
        return analysis;
    }

}