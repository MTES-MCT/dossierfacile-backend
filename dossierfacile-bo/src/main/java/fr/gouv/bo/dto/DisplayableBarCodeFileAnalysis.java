package fr.gouv.bo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.BarCodeFileAnalysis;
import fr.dossierfacile.common.entity.File;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.dossierfacile.common.entity.BarCodeType.TWO_D_DOC;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.INVALID;
import static fr.dossierfacile.common.enums.FileAuthenticationStatus.VALID;

@AllArgsConstructor
public class DisplayableBarCodeFileAnalysis {

    private final BarCodeFileAnalysis analysis;

    public static Optional<DisplayableBarCodeFileAnalysis> of(File file) {
        return Optional.ofNullable(file.getFileAnalysis())
                .map(DisplayableBarCodeFileAnalysis::new);
    }

    public String getIssuerName() {
        return analysis.getIssuerName().getLabel();
    }

    public String getAuthenticationStatusCssClass() {
        return analysis.getAuthenticationStatus() == VALID ? "fa-check text-success" : "fa-times text-danger";
    }

    public String getAuthenticationStatus() {
        return switch (analysis.getAuthenticationStatus()) {
            case VALID -> "Authentifié";
            case INVALID -> analysis.getBarCodeType() == TWO_D_DOC ? "Falsifié" : "Non authentifié";
            case API_ERROR -> "Impossible de vérifier l'authenticité auprès de l'émetteur";
            case ERROR -> "Erreur lors de l'authentification";
            case UNKNOWN_DOCUMENT -> "Non reconnu ou expiré";
        };
    }

    public boolean isNotAuthenticated() {
        return analysis.getAuthenticationStatus() == INVALID;
    }

    public boolean isInWrongCategory() {
        return !analysis.isAllowedInDocumentCategory();
    }

    @SuppressWarnings("unchecked")
    public String getAuthenticatedContent() {
        Object verifiedData = analysis.getVerifiedData();
        if (analysis.getBarCodeType() == TWO_D_DOC) {
            return formatToList((Map<String, String>) verifiedData);
        }
        return switch (analysis.getIssuerName()) {
            case MON_FRANCE_CONNECT -> formatToList((List<String>) verifiedData);
            case PAYFIT -> PayfitAuthenticatedContent.format(verifiedData);
            default -> verifiedData.toString();
        };
    }

    public boolean is2DDoc() {
        return analysis.getBarCodeType() == TWO_D_DOC;
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
            Map<String, String> map = new HashMap<>();
            map.put("Entreprise", companyName);
            map.put("Employé", employeeName);
            map.put("Salaire net", netSalary);
            return formatToList(map);
        }

    }

    private static String formatToList(List<String> data) {
        String listElements = data.stream()
                .map(element -> "<li>" + element + "</li>")
                .collect(Collectors.joining());
        return "<ul>" + listElements + "</ul>";
    }

    private static String formatToList(Map<String, String> data) {
        return formatToList(
                data.entrySet().stream()
                        .map(entry -> entry.getKey() + " : " + entry.getValue())
                        .collect(Collectors.toList())
        );
    }

}
