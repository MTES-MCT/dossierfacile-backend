package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.QrCodeFileAnalysis;
import lombok.AllArgsConstructor;

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
            case PAYFIT -> apiResponse.toString();
        };
    }

}
