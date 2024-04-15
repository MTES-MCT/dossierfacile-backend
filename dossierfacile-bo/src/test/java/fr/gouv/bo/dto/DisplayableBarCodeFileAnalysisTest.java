package fr.gouv.bo.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.BarCodeDocumentType;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.BarCodeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayableBarCodeFileAnalysisTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_format_payfit_content() throws JsonProcessingException {
        var analysis = buildAnalysis(BarCodeDocumentType.PAYFIT_PAYSLIP, BarCodeType.QR_CODE, """
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
        var analysis = buildAnalysis(BarCodeDocumentType.TAX_ASSESSMENT, BarCodeType.TWO_D_DOC, """
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

    private BarCodeFileAnalysis buildAnalysis(BarCodeDocumentType issuer, BarCodeType barCodeType, String content) throws JsonProcessingException {
        BarCodeFileAnalysis analysis = new BarCodeFileAnalysis();
        analysis.setDocumentType(issuer);
        analysis.setBarCodeType(barCodeType);
        analysis.setVerifiedData(objectMapper.readValue(content, ObjectNode.class));
        return analysis;
    }

}