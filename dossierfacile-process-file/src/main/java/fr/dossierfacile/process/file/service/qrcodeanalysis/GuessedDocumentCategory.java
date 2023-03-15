package fr.dossierfacile.process.file.service.qrcodeanalysis;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@AllArgsConstructor
public class GuessedDocumentCategory {
    private final DocumentCategory category;
    private final DocumentSubCategory subCategory;

    public boolean isMatchingCategoryOf(Document document) {
        return document.getDocumentCategory() == category &&
                document.getDocumentSubCategory() == subCategory;
    }
}