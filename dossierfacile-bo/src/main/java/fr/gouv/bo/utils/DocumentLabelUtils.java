package fr.gouv.bo.utils;

import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.stereotype.Component;

@Component
public class DocumentLabelUtils {

    private static final String UNDEFINED_LABEL = "Tous les documents";

    public String getSpecificSubCategoryLabel(DocumentSubCategory documentSubCategory) {
        return documentSubCategory == DocumentSubCategory.UNDEFINED ? UNDEFINED_LABEL : documentSubCategory.name();
    }

    public String getSpecificCategoryLabel(DocumentCategory documentCategory) {
        return documentCategory == DocumentCategory.NULL ? UNDEFINED_LABEL : documentCategory.name();
    }

    public String getDocumentUserTypeLabel(String documentUserType) {
        return switch (documentUserType) {
            case "all" -> "Tous";
            case "guarantor" -> "Garant";
            case "tenant" -> "Locataire";
            default -> documentUserType;
        };
    }

    public static String getDocumentAnalysStatusLabel(DocumentAnalysisStatus documentAnalysisStatus) {
        return switch (documentAnalysisStatus) {
            case UNDEFINED -> "Analyse incomplète";
            case CHECKED -> "Analyse complète";
            case DENIED -> "Anomalie détectée";
        };
    }

    public static String getDocumentAnalysStatusSubLabel(DocumentAnalysisStatus documentAnalysisStatus) {
        return switch (documentAnalysisStatus) {
            case UNDEFINED -> "- Le système n’a pas pu analyser tout le document.";
            case CHECKED -> "- Toutes les règles ont été validées";
            case DENIED -> "- Une ou plusieurs règles ont été rejetées.";
        };
    }

}
