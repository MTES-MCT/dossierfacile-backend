package fr.gouv.bo.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.BarCodeType;
import fr.dossierfacile.common.entity.DocumentIssuer;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayableBarCodeFileAnalysisTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_format_mon_france_connect_content() throws JsonProcessingException {
        var analysis = buildAnalysis(DocumentIssuer.MON_FRANCE_CONNECT, BarCodeType.QR_CODE, """
                ["2021", "Angela Claire Louise, DUBOIS", "Philippe, DUBOIS", "Marié(e)", "20 avenue de Ségur", "82357   €", "88185   €", "2,5", "1,0"]
                """);

        var displayableAnalysis = new DisplayableBarCodeFileAnalysis(analysis);

        assertThat(displayableAnalysis.getAuthenticatedContent())
                .isEqualToIgnoringWhitespace("""
                        <ul>
                            <li>2021</li>
                            <li>Angela Claire Louise, DUBOIS</li>
                            <li>Philippe, DUBOIS</li>
                            <li>Marié(e)</li>
                            <li>20 avenue de Ségur</li>
                            <li>82357   €</li>
                            <li>88185   €</li>
                            <li>2,5</li>
                            <li>1,0</li>
                        </ul>
                        """);
    }

    @Test
    void should_format_payfit_content() throws JsonProcessingException {
        var analysis = buildAnalysis(DocumentIssuer.PAYFIT, BarCodeType.QR_CODE, """
                {
                    "netSalary": "1 895,39 €",
                    "companyName": "Some Company",
                    "grossSalary": "2 533,33 €",
                    "companySiret": "128759437",
                    "employeeName": "John Doe"
                }
                """);

        var displayableAnalysis = new DisplayableBarCodeFileAnalysis(analysis);

        assertThat(displayableAnalysis.getAuthenticatedContent())
                .isEqualToIgnoringWhitespace("""
                        <ul>
                            <li>Entreprise : Some Company</li>
                            <li>Employé : John Doe</li>
                            <li>Salaire net : 1 895,39 €</li>
                        </ul>
                        """);
    }

    @Test
    void should_format_2ddoc_content() throws JsonProcessingException {
        var analysis = buildAnalysis(DocumentIssuer.DGFIP, BarCodeType.TWO_D_DOC, """
                {
                    "Revenu fiscal de référence": "1234",
                    "Nom": "Doe"
                }
                """);

        var displayableAnalysis = new DisplayableBarCodeFileAnalysis(analysis);

        assertThat(displayableAnalysis.getAuthenticatedContent())
                .isEqualToIgnoringWhitespace("""
                        <ul>
                            <li>Revenu fiscal de référence : 1234</li>
                            <li>Nom : Doe</li>
                        </ul>
                        """);
    }

    private BarCodeFileAnalysis buildAnalysis(DocumentIssuer issuer, BarCodeType barCodeType, String content) throws JsonProcessingException {
        BarCodeFileAnalysis analysis = new BarCodeFileAnalysis();
        analysis.setIssuerName(issuer);
        analysis.setBarCodeType(barCodeType);
        analysis.setVerifiedData(objectMapper.readValue(content, Object.class));
        return analysis;
    }

}