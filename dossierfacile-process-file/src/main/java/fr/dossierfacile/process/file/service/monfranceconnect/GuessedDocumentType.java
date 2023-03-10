package fr.dossierfacile.process.file.service.monfranceconnect;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.process.file.util.InMemoryPdfFile;
import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public enum GuessedDocumentType {

    TAX("Justificatif de revenus imposables"),
    SCHOLARSHIP("Justificatif de statut étudiant boursier"),
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
            case TAX -> document.getDocumentCategory() == DocumentCategory.TAX;
            case SCHOLARSHIP -> document.getDocumentSubCategory() == DocumentSubCategory.SCHOLARSHIP;
            case UNKNOWN -> true;
        };
    }

}
