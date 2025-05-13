package fr.gouv.bo.utils;

import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.stereotype.Component;

@Component
public class DocumentSubCategoryUtils {

    public String getSpecificLabel(DocumentSubCategory documentSubCategory) {
        return documentSubCategory == DocumentSubCategory.UNDEFINED ? "Tous les documents" : documentSubCategory.name();
    }

    public String getDocumentUserTypeLabel(String documentUserType) {
        return switch (documentUserType) {
            case "all" -> "Tous";
            case "guarantor" -> "Garant";
            case "tenant" -> "Locataire";
            default -> documentUserType;
        };
    }

}
