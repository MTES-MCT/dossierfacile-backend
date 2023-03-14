package fr.dossierfacile.process.file.service.monfranceconnect;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import lombok.AllArgsConstructor;

import java.util.Arrays;

import static fr.dossierfacile.common.enums.DocumentCategory.FINANCIAL;
import static fr.dossierfacile.common.enums.DocumentCategory.PROFESSIONAL;
import static fr.dossierfacile.common.enums.DocumentCategory.TAX;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SOCIAL_SERVICE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.STUDENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.UNEMPLOYED;

@AllArgsConstructor
public enum GuessedDocumentType {

    TAXABLE_INCOME("Justificatif de revenus imposables"),
    SCHOLARSHIP("Justificatif de statut étudiant boursier"),
    STUDENT_STATUS("Justificatif de statut étudiant"),
    UNEMPLOYMENT_STATUS("Justificatif d'inscription à Pôle emploi"),
    UNEMPLOYMENT_BENEFIT("Justificatif d'indemnisation à Pôle emploi"),
    UNKNOWN("")
    ;

    private final String documentTitle;

    public static GuessedDocumentType of(InMemoryPdfFile file) {
        String contentAsString = file.readContentAsString();
        return Arrays.stream(GuessedDocumentType.values())
                .filter(type -> !type.equals(UNKNOWN))
                .filter(type -> contentAsString.contains(type.documentTitle))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public boolean isMatchingCategoryOf(Document document) {
        return switch (this) {
            case TAXABLE_INCOME -> hasCategory(document, TAX, MY_NAME);
            case SCHOLARSHIP -> hasCategory(document, FINANCIAL, DocumentSubCategory.SCHOLARSHIP);
            case STUDENT_STATUS -> hasCategory(document, PROFESSIONAL, STUDENT);
            case UNEMPLOYMENT_STATUS -> hasCategory(document, PROFESSIONAL, UNEMPLOYED);
            case UNEMPLOYMENT_BENEFIT -> hasCategory(document, FINANCIAL, SOCIAL_SERVICE);
            case UNKNOWN -> true;
        };
    }

    private static boolean hasCategory(Document document, DocumentCategory category, DocumentSubCategory subCategory) {
        return document.getDocumentCategory() == category &&
                document.getDocumentSubCategory() == subCategory;
    }

}
