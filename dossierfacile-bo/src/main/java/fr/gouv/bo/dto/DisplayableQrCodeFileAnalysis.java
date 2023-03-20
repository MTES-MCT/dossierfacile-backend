package fr.gouv.bo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static fr.dossierfacile.common.enums.FileAuthenticationStatus.INVALID;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.VALID;

@AllArgsConstructor
public class DisplayableQrCodeFileAnalysis {

    private final int order;
    private final QrCodeFileAnalysis analysis;

    public String getSummary() {
        return "Fichier n°" + order + " (" + analysis.getIssuerName().getLabel() + ") :";
    }

    public String getAuthenticationStatusCssClass() {
        return analysis.getAuthenticationStatus() == VALID ? "fa-check text-success" : "fa-times text-danger";
    }

    public String getAuthenticationStatus() {
        return analysis.getAuthenticationStatus().getLabel();
    }

    public boolean isNotAuthenticated() {
        return analysis.getAuthenticationStatus() == INVALID;
    }

    public boolean isInWrongCategory() {
        return !analysis.isAllowedInDocumentCategory();
    }

    @SuppressWarnings("unchecked")
    public String getAuthenticatedContent() {
        Object apiResponse = analysis.getApiResponse();
        return switch (analysis.getIssuerName()) {
            case MON_FRANCE_CONNECT -> String.join(", ", (List<String>) apiResponse);
            case PAYFIT -> PayfitAuthenticatedContent.format(apiResponse);
        };
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PayfitAuthenticatedContent {

        private static final ObjectMapper objectMapper = new ObjectMapper();

        private String companyName;
        private String employeeName;
        private String netSalary;

        static String format(Object object) {
            try {
                String json = objectMapper.writeValueAsString(object);
                var content = objectMapper.readValue(json, PayfitAuthenticatedContent.class);
                return content.toString();
            } catch (JsonProcessingException e) {
                return "";
            }
        }

        @Override
        public String toString() {
            return "entreprise = " + companyName +
                    ", employé = " + employeeName +
                    ", net = " + netSalary;
        }

    }

}
